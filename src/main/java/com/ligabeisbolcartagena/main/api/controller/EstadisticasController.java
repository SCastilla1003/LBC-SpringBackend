package com.ligabeisbolcartagena.main.api.controller;

import com.ligabeisbolcartagena.main.api.dto.EstadisticasDtos.EnfrentamientoDto;
import com.ligabeisbolcartagena.main.api.dto.EstadisticasDtos.EstadisticasJugadorDto;
import com.ligabeisbolcartagena.main.api.dto.EstadisticasDtos.FilaBateoDto;
import com.ligabeisbolcartagena.main.api.dto.EstadisticasDtos.FilaPitcheoDto;
import com.ligabeisbolcartagena.main.service.EstadisticasService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Estadísticas calculadas con las jugadas del anotador. Todo es público. */
@RestController
@RequestMapping("/api/v1")
public class EstadisticasController {

    private final EstadisticasService estadisticas;

    public EstadisticasController(EstadisticasService estadisticas) {
        this.estadisticas = estadisticas;
    }

    /** Bateo y pitcheo del jugador en la temporada y en toda su carrera. */
    @GetMapping("/temporadas/{temporadaId}/jugadores/{jugadorId}/estadisticas")
    public EstadisticasJugadorDto deJugador(@PathVariable Long temporadaId, @PathVariable Long jugadorId) {
        return estadisticas.deJugador(temporadaId, jugadorId);
    }

    @GetMapping("/temporadas/{temporadaId}/estadisticas/bateo")
    public List<FilaBateoDto> bateo(@PathVariable Long temporadaId) {
        return estadisticas.bateo(temporadaId);
    }

    @GetMapping("/temporadas/{temporadaId}/estadisticas/pitcheo")
    public List<FilaPitcheoDto> pitcheo(@PathVariable Long temporadaId) {
        return estadisticas.pitcheo(temporadaId);
    }

    /** Historial del bateador contra el lanzador y probabilidad de hit, embasarse y ponche. */
    @GetMapping("/enfrentamientos")
    public EnfrentamientoDto enfrentamiento(@RequestParam Long bateadorId, @RequestParam Long lanzadorId,
                                            @RequestParam(required = false) Long temporadaId) {
        return estadisticas.enfrentamiento(bateadorId, lanzadorId, temporadaId);
    }
}
