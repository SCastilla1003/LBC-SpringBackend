package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.EquipoDtos.EquipoDto;
import com.ligabeisbolcartagena.main.api.dto.JugadorDtos.JugadorDto;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.api.error.CodigoError;
import com.ligabeisbolcartagena.main.dominio.Equipo;
import com.ligabeisbolcartagena.main.dominio.EquipoRepository;
import com.ligabeisbolcartagena.main.dominio.Jugador;
import com.ligabeisbolcartagena.main.dominio.JugadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FotoServiceTest {

    /** Almacén en memoria en lugar del bucket de Railway. */
    static class AlmacenEnMemoria implements AlmacenObjetos {
        final Map<String, Objeto> objetos = new HashMap<>();
        boolean disponible = true;

        @Override
        public boolean disponible() {
            return disponible;
        }

        @Override
        public void guardar(String clave, byte[] datos, String tipo) {
            objetos.put(clave, new Objeto(datos, tipo));
        }

        @Override
        public Optional<Objeto> leer(String clave) {
            return Optional.ofNullable(objetos.get(clave));
        }

        @Override
        public void borrar(String clave) {
            objetos.remove(clave);
        }
    }

    static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 1, 2, 3};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    AlmacenEnMemoria almacen;
    JugadorRepository jugadores;
    EquipoRepository equipos;
    FotoService servicio;
    Jugador jugador;
    Equipo equipo;

    @BeforeEach
    void preparar() {
        almacen = new AlmacenEnMemoria();
        jugadores = mock(JugadorRepository.class);
        equipos = mock(EquipoRepository.class);
        jugador = new Jugador();
        jugador.setId(17L);
        jugador.setNombres("Luis");
        jugador.setApellidos("Calvo");
        equipo = new Equipo();
        equipo.setId(4L);
        when(jugadores.findById(17L)).thenReturn(Optional.of(jugador));
        when(equipos.findById(4L)).thenReturn(Optional.of(equipo));
        when(jugadores.save(any(Jugador.class))).thenAnswer(i -> i.getArgument(0));
        when(equipos.save(any(Equipo.class))).thenAnswer(i -> i.getArgument(0));
        Clock reloj = Clock.fixed(Instant.ofEpochMilli(1_700_000_000_000L), ZoneOffset.UTC);
        servicio = new FotoService(almacen, jugadores, equipos, new Mapeador("America/Bogota"), reloj);
    }

    @Test
    void subirFotoGuardaEnElBucketYApuntaALaApi() {
        JugadorDto dto = servicio.subirFotoJugador(17L, JPG);

        assertThat(almacen.objetos.get("jugadores/17").tipo()).isEqualTo("image/jpeg");
        assertThat(dto.fotoUrl()).isEqualTo("/api/v1/jugadores/17/foto?v=1700000000000");
        assertThat(servicio.fotoJugador(17L).datos()).isEqualTo(JPG);
    }

    @Test
    void escudoPngYBorrado() {
        EquipoDto dto = servicio.subirEscudo(4L, PNG);
        assertThat(dto.escudoUrl()).startsWith("/api/v1/equipos/4/escudo?v=");
        assertThat(almacen.objetos.get("equipos/4").tipo()).isEqualTo("image/png");

        EquipoDto borrado = servicio.borrarEscudo(4L);
        assertThat(borrado.escudoUrl()).isNull();
        assertThat(almacen.objetos).doesNotContainKey("equipos/4");
    }

    @Test
    void rechazaLoQueNoEsImagen() {
        assertThatThrownBy(() -> servicio.subirFotoJugador(17L, "hola".getBytes()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCodigo())
                .isEqualTo(CodigoError.TIPO_NO_SOPORTADO);
        assertThat(almacen.objetos).isEmpty();
    }

    @Test
    void rechazaImagenDemasiadoGrande() {
        byte[] grande = new byte[FotoService.MAX_BYTES + 1];
        System.arraycopy(JPG, 0, grande, 0, JPG.length);
        assertThatThrownBy(() -> servicio.subirFotoJugador(17L, grande))
                .extracting(e -> ((ApiException) e).getCodigo())
                .isEqualTo(CodigoError.ARCHIVO_DEMASIADO_GRANDE);
    }

    @Test
    void sinBucketConfiguradoAvisaQueNoHayServicio() {
        almacen.disponible = false;
        assertThatThrownBy(() -> servicio.subirFotoJugador(17L, JPG))
                .extracting(e -> ((ApiException) e).getCodigo())
                .isEqualTo(CodigoError.SERVICIO_NO_DISPONIBLE);
    }

    @Test
    void fotoInexistenteEs404() {
        assertThatThrownBy(() -> servicio.fotoJugador(17L))
                .extracting(e -> ((ApiException) e).getCodigo())
                .isEqualTo(CodigoError.NO_ENCONTRADO);
    }

    @Test
    void reconoceWebp() {
        byte[] webp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        assertThat(FotoService.tipoImagen(webp)).isEqualTo("image/webp");
        assertThat(FotoService.tipoImagen(new byte[] {1, 2})).isNull();
    }
}
