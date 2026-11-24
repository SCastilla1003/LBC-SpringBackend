package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.EquipoDtos.EquipoDto;
import com.ligabeisbolcartagena.main.api.dto.JugadorDtos.JugadorDto;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.api.error.CodigoError;
import com.ligabeisbolcartagena.main.dominio.Equipo;
import com.ligabeisbolcartagena.main.dominio.EquipoRepository;
import com.ligabeisbolcartagena.main.dominio.Jugador;
import com.ligabeisbolcartagena.main.dominio.JugadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Fotos de jugadores y escudos de equipos guardados en el bucket.
 *
 * Cada imagen vive en una clave fija (jugadores/{id}, equipos/{id}); subir otra la
 * reemplaza. La URL guardada apunta a la propia API (el bucket es privado) y lleva
 * ?v=marca-de-tiempo para que la app no muestre la versión anterior desde caché.
 */
@Service
public class FotoService {

    /** Tamaño máximo de una imagen ya comprimida por la app. */
    static final int MAX_BYTES = 5 * 1024 * 1024;

    private final AlmacenObjetos almacen;
    private final JugadorRepository jugadores;
    private final EquipoRepository equipos;
    private final Mapeador mapeador;
    private final Clock reloj;

    @Autowired
    public FotoService(AlmacenObjetos almacen, JugadorRepository jugadores, EquipoRepository equipos,
                       Mapeador mapeador) {
        this(almacen, jugadores, equipos, mapeador, Clock.systemUTC());
    }

    FotoService(AlmacenObjetos almacen, JugadorRepository jugadores, EquipoRepository equipos, Mapeador mapeador,
                Clock reloj) {
        this.almacen = almacen;
        this.jugadores = jugadores;
        this.equipos = equipos;
        this.mapeador = mapeador;
        this.reloj = reloj;
    }

    static String claveJugador(Long id) {
        return "jugadores/" + id;
    }

    static String claveEquipo(Long id) {
        return "equipos/" + id;
    }

    @Transactional
    public JugadorDto subirFotoJugador(Long id, byte[] datos) {
        Jugador j = jugador(id);
        guardar(claveJugador(id), datos);
        j.setFotoUrl("/api/v1/jugadores/" + id + "/foto?v=" + reloj.millis());
        return mapeador.dto(jugadores.save(j));
    }

    @Transactional
    public JugadorDto borrarFotoJugador(Long id) {
        Jugador j = jugador(id);
        if (almacen.disponible()) {
            almacen.borrar(claveJugador(id));
        }
        j.setFotoUrl(null);
        return mapeador.dto(jugadores.save(j));
    }

    @Transactional(readOnly = true)
    public AlmacenObjetos.Objeto fotoJugador(Long id) {
        jugador(id);
        return leer(claveJugador(id), "Foto del jugador", id);
    }

    @Transactional
    public EquipoDto subirEscudo(Long id, byte[] datos) {
        Equipo e = equipo(id);
        guardar(claveEquipo(id), datos);
        e.setEscudoUrl("/api/v1/equipos/" + id + "/escudo?v=" + reloj.millis());
        return mapeador.dto(equipos.save(e));
    }

    @Transactional
    public EquipoDto borrarEscudo(Long id) {
        Equipo e = equipo(id);
        if (almacen.disponible()) {
            almacen.borrar(claveEquipo(id));
        }
        e.setEscudoUrl(null);
        return mapeador.dto(equipos.save(e));
    }

    @Transactional(readOnly = true)
    public AlmacenObjetos.Objeto escudo(Long id) {
        equipo(id);
        return leer(claveEquipo(id), "Escudo del equipo", id);
    }

    private void guardar(String clave, byte[] datos) {
        exigirAlmacen();
        if (datos == null || datos.length == 0) {
            throw ApiException.invalida("Falta la imagen (campo 'archivo')");
        }
        if (datos.length > MAX_BYTES) {
            throw new ApiException(CodigoError.ARCHIVO_DEMASIADO_GRANDE, "La imagen no puede pasar de 5 MB");
        }
        String tipo = tipoImagen(datos);
        if (tipo == null) {
            throw new ApiException(CodigoError.TIPO_NO_SOPORTADO, "La imagen debe ser JPG, PNG o WEBP");
        }
        almacen.guardar(clave, datos, tipo);
    }

    private AlmacenObjetos.Objeto leer(String clave, String recurso, Long id) {
        exigirAlmacen();
        return almacen.leer(clave).orElseThrow(() -> ApiException.noEncontrado(recurso, String.valueOf(id)));
    }

    private void exigirAlmacen() {
        if (!almacen.disponible()) {
            throw new ApiException(CodigoError.SERVICIO_NO_DISPONIBLE,
                    "El almacenamiento de fotos no está configurado en el servidor");
        }
    }

    /** Tipo de imagen según su firma (no se confía en la extensión ni en el Content-Type). */
    static String tipoImagen(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') {
            return "image/png";
        }
        if (d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private Jugador jugador(Long id) {
        return jugadores.findById(id).orElseThrow(() -> ApiException.noEncontrado("Jugador", String.valueOf(id)));
    }

    private Equipo equipo(Long id) {
        return equipos.findById(id).orElseThrow(() -> ApiException.noEncontrado("Equipo", String.valueOf(id)));
    }
}
