package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.PartidoDtos.LineScoreDto;
import com.ligabeisbolcartagena.main.dominio.Mitad;
import com.ligabeisbolcartagena.main.dominio.Partido;
import com.ligabeisbolcartagena.main.service.LineScoreBuilder.CarreraMitad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LineScoreBuilderTest {

    @Test
    void partidoProgramadoMuestraCasillasVacias() {
        LineScoreDto r = LineScoreBuilder.construir(9, Partido.Estado.SCHEDULED, 1, Mitad.TOP, List.of());

        assertThat(r.entradas()).hasSize(9);
        assertThat(r.entradas()).allSatisfy(e -> {
            assertThat(e.local()).isNull();
            assertThat(e.visita()).isNull();
        });
        assertThat(r.entradasExtra()).isFalse();
    }

    @Test
    void partidoEnVivoMuestraLasEntradasSinJugarEnNull() {
        List<CarreraMitad> carreras = List.of(new CarreraMitad(1, Mitad.TOP, 2), new CarreraMitad(2, Mitad.BOTTOM, 1));

        LineScoreDto r = LineScoreBuilder.construir(9, Partido.Estado.LIVE, 2, Mitad.BOTTOM, carreras);

        assertThat(r.entradas()).hasSize(9);
        assertThat(r.entradas().get(0).visita()).isEqualTo(2);
        assertThat(r.entradas().get(0).local()).isEqualTo(0);
        assertThat(r.entradas().get(1).visita()).isEqualTo(0);
        assertThat(r.entradas().get(1).local()).isEqualTo(1);
        assertThat(r.entradas().get(2).visita()).isNull();
        assertThat(r.entradas().get(2).local()).isNull();
    }

    @Test
    void partidoTerminadoEnLaSeptimaMuestraSieteEntradas() {
        List<CarreraMitad> carreras = List.of(new CarreraMitad(7, Mitad.BOTTOM, 3), new CarreraMitad(7, Mitad.TOP, 1));

        LineScoreDto r = LineScoreBuilder.construir(9, Partido.Estado.FINISHED, 7, Mitad.BOTTOM, carreras);

        assertThat(r.entradas()).hasSize(7);
        assertThat(r.entradas().get(6).local()).isEqualTo(3);
        assertThat(r.entradas().get(6).visita()).isEqualTo(1);
        assertThat(r.entradasExtra()).isFalse();
    }

    @Test
    void partidoQueSuperaLasEntradasProgramadasSeMarcaComoExtra() {
        LineScoreDto r = LineScoreBuilder.construir(9, Partido.Estado.FINISHED, 10, Mitad.BOTTOM, List.of());

        assertThat(r.entradas()).hasSize(10);
        assertThat(r.entradasExtra()).isTrue();
        assertThat(r.entradasProgramadas()).isEqualTo(9);
    }
}
