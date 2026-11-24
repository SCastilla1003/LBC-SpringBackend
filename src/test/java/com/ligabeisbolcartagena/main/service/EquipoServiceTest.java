package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.ComunDtos.EquipoResumen;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.api.error.CodigoError;
import com.ligabeisbolcartagena.main.dominio.Equipo;
import com.ligabeisbolcartagena.main.dominio.EquipoRepository;
import com.ligabeisbolcartagena.main.dominio.InscripcionEquipo;
import com.ligabeisbolcartagena.main.dominio.InscripcionEquipoRepository;
import com.ligabeisbolcartagena.main.dominio.PartidoRepository;
import com.ligabeisbolcartagena.main.dominio.Plantilla;
import com.ligabeisbolcartagena.main.dominio.PlantillaRepository;
import com.ligabeisbolcartagena.main.dominio.Temporada;
import com.ligabeisbolcartagena.main.dominio.TemporadaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EquipoServiceTest {

    EquipoRepository equipos;
    PlantillaRepository plantillas;
    TemporadaRepository temporadas;
    InscripcionEquipoRepository inscripciones;
    PartidoRepository partidos;
    EquipoService servicio;
    Temporada temporada;
    Equipo yankees;
    Equipo tigres;

    static Equipo equipo(long id, String nombre) {
        Equipo e = new Equipo();
        e.setId(id);
        e.setNombre(nombre);
        return e;
    }

    @BeforeEach
    void preparar() {
        equipos = mock(EquipoRepository.class);
        plantillas = mock(PlantillaRepository.class);
        temporadas = mock(TemporadaRepository.class);
        inscripciones = mock(InscripcionEquipoRepository.class);
        partidos = mock(PartidoRepository.class);
        servicio = new EquipoService(equipos, plantillas, temporadas, inscripciones, partidos,
                new Mapeador("America/Bogota"));
        temporada = new Temporada();
        yankees = equipo(4, "New York Yankees");
        tigres = equipo(5, "Tigres");
        when(temporadas.existsById(1L)).thenReturn(true);
        when(temporadas.findById(1L)).thenReturn(Optional.of(temporada));
        when(equipos.findById(4L)).thenReturn(Optional.of(yankees));
        when(equipos.findById(5L)).thenReturn(Optional.of(tigres));
    }

    @Test
    void equiposDeTemporadaUneInscritosSinJugadoresYLosQueTienenPlantilla() {
        Plantilla p = new Plantilla();
        p.setEquipo(yankees);
        when(plantillas.findByTemporadaId(1L)).thenReturn(List.of(p));
        when(inscripciones.findByTemporadaId(1L))
                .thenReturn(List.of(new InscripcionEquipo(temporada, tigres), new InscripcionEquipo(temporada, yankees)));

        List<EquipoResumen> r = servicio.deTemporada(1L);

        assertThat(r).extracting(EquipoResumen::nombre).containsExactly("New York Yankees", "Tigres");
    }

    @Test
    void inscribirDosVecesEsConflicto() {
        when(inscripciones.existsByTemporadaIdAndEquipoId(1L, 4L)).thenReturn(true);
        assertThatThrownBy(() -> servicio.inscribir(1L, 4L))
                .extracting(e -> ((ApiException) e).getCodigo())
                .isEqualTo(CodigoError.CONFLICTO);
        verify(inscripciones, never()).save(any());
    }

    @Test
    void inscribirGuardaLaInscripcion() {
        EquipoResumen r = servicio.inscribir(1L, 5L);
        assertThat(r.nombre()).isEqualTo("Tigres");
        verify(inscripciones).save(any(InscripcionEquipo.class));
    }

    @Test
    void noSeQuitaUnEquipoConJugadores() {
        when(plantillas.findByTemporadaIdAndEquipoId(1L, 4L)).thenReturn(List.of(new Plantilla()));
        assertThatThrownBy(() -> servicio.desinscribir(1L, 4L))
                .extracting(e -> ((ApiException) e).getCodigo())
                .isEqualTo(CodigoError.CONFLICTO);
    }

    @Test
    void quitarUnEquipoSinJugadoresNiPartidosBorraLaInscripcion() {
        InscripcionEquipo i = new InscripcionEquipo(temporada, tigres);
        when(plantillas.findByTemporadaIdAndEquipoId(1L, 5L)).thenReturn(List.of());
        when(partidos.findByTemporadaIdOrderByFechaHoraAsc(1L)).thenReturn(List.of());
        when(inscripciones.findByTemporadaIdAndEquipoId(1L, 5L)).thenReturn(Optional.of(i));
        servicio.desinscribir(1L, 5L);
        verify(inscripciones).delete(i);
    }
}
