package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.JugadorDtos.PlantillaActualizacion;
import com.ligabeisbolcartagena.main.api.dto.JugadorDtos.PlantillaDto;
import com.ligabeisbolcartagena.main.api.dto.JugadorDtos.PlantillaRequest;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.dominio.Equipo;
import com.ligabeisbolcartagena.main.dominio.EquipoRepository;
import com.ligabeisbolcartagena.main.dominio.Jugador;
import com.ligabeisbolcartagena.main.dominio.JugadorRepository;
import com.ligabeisbolcartagena.main.dominio.Plantilla;
import com.ligabeisbolcartagena.main.dominio.PlantillaRepository;
import com.ligabeisbolcartagena.main.dominio.PosicionAlineacionRepository;
import com.ligabeisbolcartagena.main.dominio.Temporada;
import com.ligabeisbolcartagena.main.dominio.TemporadaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Inscripción de jugadores en la temporada: equipo, número y posición. */
@Service
public class PlantillaService {

    private AuditoriaService auditoria = AuditoriaService.NINGUNA;

    @org.springframework.beans.factory.annotation.Autowired
    void setAuditoria(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    private final PlantillaRepository plantillas;
    private final TemporadaRepository temporadas;
    private final JugadorRepository jugadores;
    private final EquipoRepository equipos;
    private final PosicionAlineacionRepository alineaciones;
    private final Mapeador mapeador;

    public PlantillaService(PlantillaRepository plantillas, TemporadaRepository temporadas,
                            JugadorRepository jugadores, EquipoRepository equipos,
                            PosicionAlineacionRepository alineaciones, Mapeador mapeador) {
        this.plantillas = plantillas;
        this.temporadas = temporadas;
        this.jugadores = jugadores;
        this.equipos = equipos;
        this.alineaciones = alineaciones;
        this.mapeador = mapeador;
    }

    @Transactional(readOnly = true)
    public List<PlantillaDto> deEquipo(Long temporadaId, Long equipoId) {
        validarTemporada(temporadaId);
        equipos.findById(equipoId).orElseThrow(() -> ApiException.noEncontrado("Equipo", String.valueOf(equipoId)));
        return plantillas.findByTemporadaIdAndEquipoId(temporadaId, equipoId).stream()
                .sorted((a, b) -> comparar(a, b))
                .map(mapeador::dto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlantillaDto> agentesLibres(Long temporadaId) {
        validarTemporada(temporadaId);
        return plantillas.findByTemporadaIdAndEquipoIsNull(temporadaId).stream()
                .sorted((a, b) -> comparar(a, b))
                .map(mapeador::dto)
                .toList();
    }

    @Transactional
    public PlantillaDto inscribir(Long temporadaId, PlantillaRequest req) {
        Temporada temporada = validarTemporada(temporadaId);
        Jugador jugador = jugadores.findById(req.jugadorId())
                .orElseThrow(() -> ApiException.noEncontrado("Jugador", String.valueOf(req.jugadorId())));
        if (plantillas.existsByTemporadaIdAndJugadorId(temporadaId, jugador.getId())) {
            throw ApiException.conflicto("El jugador ya está inscrito en esta temporada. Para cambiarlo, edita su entrada.");
        }
        Plantilla p = new Plantilla();
        p.setTemporada(temporada);
        p.setJugador(jugador);
        p.setEquipo(equipoOpcional(req.equipoId()));
        p.setNumero(req.numero());
        p.setPosicion(req.posicion());
        var dto = mapeador.dto(plantillas.save(p));
        auditoria.registrar("INSCRIBIR", "Plantilla", dto.id(), "Inscribió a "
                + AuditoriaService.nombre(jugador.getNombres(), jugador.getApellidos()) + " en "
                + (p.getEquipo() == null ? "agentes libres" : p.getEquipo().getNombre()), null, dto);
        return dto;
    }

    @Transactional
    public PlantillaDto actualizar(Long temporadaId, Long entradaId, PlantillaActualizacion req) {
        Plantilla p = buscar(temporadaId, entradaId);
        var antes = mapeador.dto(p);
        String equipoAntes = p.getEquipo() == null ? "agentes libres" : p.getEquipo().getNombre();
        p.setEquipo(equipoOpcional(req.equipoId()));
        p.setNumero(req.numero());
        p.setPosicion(req.posicion());
        var dto = mapeador.dto(plantillas.save(p));
        String equipoAhora = p.getEquipo() == null ? "agentes libres" : p.getEquipo().getNombre();
        String quien = AuditoriaService.nombre(p.getJugador().getNombres(), p.getJugador().getApellidos());
        auditoria.registrar(equipoAntes.equals(equipoAhora) ? "EDITAR" : "MOVER", "Plantilla", entradaId,
                equipoAntes.equals(equipoAhora) ? "Editó a " + quien + " en " + equipoAhora
                        : "Movió a " + quien + " de " + equipoAntes + " a " + equipoAhora, antes, dto);
        return dto;
    }

    @Transactional
    public void quitar(Long temporadaId, Long entradaId) {
        Plantilla p = buscar(temporadaId, entradaId);
        if (alineaciones.countByJugadorIdAndPartidoTemporadaId(p.getJugador().getId(), temporadaId) > 0) {
            throw ApiException.conflicto("El jugador ya aparece en alineaciones de esta temporada");
        }
        var antes = mapeador.dto(p);
        try {
            plantillas.delete(p);
            plantillas.flush();
            auditoria.registrar("QUITAR", "Plantilla", entradaId, "Quitó a "
                    + AuditoriaService.nombre(p.getJugador().getNombres(), p.getJugador().getApellidos())
                    + " del roster de " + (p.getEquipo() == null ? "agentes libres" : p.getEquipo().getNombre()),
                    antes, null);
        } catch (DataIntegrityViolationException e) {
            throw Errores.relacionado();
        }
    }

    private Plantilla buscar(Long temporadaId, Long entradaId) {
        validarTemporada(temporadaId);
        return plantillas.findByIdAndTemporadaId(entradaId, temporadaId)
                .orElseThrow(() -> ApiException.noEncontrado("Entrada de plantilla", String.valueOf(entradaId)));
    }

    private Temporada validarTemporada(Long temporadaId) {
        return temporadas.findById(temporadaId)
                .orElseThrow(() -> ApiException.noEncontrado("Temporada", String.valueOf(temporadaId)));
    }

    private Equipo equipoOpcional(Long equipoId) {
        if (equipoId == null) {
            return null;
        }
        return equipos.findById(equipoId)
                .orElseThrow(() -> ApiException.noEncontrado("Equipo", String.valueOf(equipoId)));
    }

    private static int comparar(Plantilla a, Plantilla b) {
        int porApellido = a.getJugador().getApellidos().compareToIgnoreCase(b.getJugador().getApellidos());
        return porApellido != 0 ? porApellido : a.getJugador().getNombres().compareToIgnoreCase(b.getJugador().getNombres());
    }
}
