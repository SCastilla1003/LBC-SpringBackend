package com.ligabeisbolcartagena.main.service.estadisticas;

import com.ligabeisbolcartagena.main.dominio.Jugada.Resultado;
import com.ligabeisbolcartagena.main.dominio.Mitad;
import com.ligabeisbolcartagena.main.dominio.Partido;
import com.ligabeisbolcartagena.main.service.estadisticas.CalculadoraEstadisticas.Bateo;
import com.ligabeisbolcartagena.main.service.estadisticas.CalculadoraEstadisticas.Calculo;
import com.ligabeisbolcartagena.main.service.estadisticas.CalculadoraEstadisticas.FilaJugada;
import com.ligabeisbolcartagena.main.service.estadisticas.CalculadoraEstadisticas.FilaMovimiento;
import com.ligabeisbolcartagena.main.service.estadisticas.CalculadoraEstadisticas.Pitcheo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.ligabeisbolcartagena.main.dominio.Jugada.Resultado.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Partido 1 (terminado 3-1): Visita (equipo 2) batea en la alta, lanza 200 por el local;
 * Local (equipo 1) batea en la baja, lanzan 100 y luego el relevista 110 por la visita.
 */
class CalculadoraEstadisticasTest {

    final List<FilaJugada> jugadas = new ArrayList<>();
    final List<FilaMovimiento> movimientos = new ArrayList<>();
    int secuencia;

    void jugada(int entrada, Mitad mitad, long bateador, long lanzador, Resultado r, int carreras, int outs,
                FilaMovimiento... movs) {
        long id = ++secuencia;
        jugadas.add(new FilaJugada(id, 1, secuencia, entrada, mitad, bateador, lanzador, r, carreras, outs, 1, 2,
                LocalDateTime.of(2026, 11, 28, 8, 0), Partido.Estado.FINISHED, 1, 3));
        for (FilaMovimiento m : movs) {
            movimientos.add(new FilaMovimiento(id, m.jugadorId(), m.desde(), m.hasta()));
        }
    }

    static FilaMovimiento mov(long jugador, int desde, Integer hasta) {
        return new FilaMovimiento(0, jugador, desde, hasta);
    }

    Calculo partido() {
        // Alta 1: sencillo de 101, robo, jonrón de 102 (2 carreras), ponche, base por bolas, elevado de sacrificio.
        jugada(1, Mitad.TOP, 101, 200, SINGLE, 0, 0, mov(101, 0, 1));
        jugada(1, Mitad.TOP, 102, 200, STOLEN_BASE, 0, 0, mov(101, 1, 2));
        jugada(1, Mitad.TOP, 102, 200, HOME_RUN, 2, 0, mov(102, 0, 4), mov(101, 2, 4));
        jugada(1, Mitad.TOP, 103, 200, STRIKEOUT, 0, 1, mov(103, 0, null));
        jugada(1, Mitad.TOP, 104, 200, WALK, 0, 0, mov(104, 0, 1));
        jugada(1, Mitad.TOP, 105, 200, DOUBLE_PLAY, 0, 2, mov(105, 0, null), mov(104, 1, null));
        // Baja 1: el local anota una con error y elevado de sacrificio.
        jugada(1, Mitad.BOTTOM, 201, 100, REACHED_ON_ERROR, 0, 0, mov(201, 0, 1));
        jugada(1, Mitad.BOTTOM, 202, 100, DOUBLE, 1, 0, mov(202, 0, 2), mov(201, 1, 4));
        jugada(1, Mitad.BOTTOM, 203, 100, STRIKEOUT, 0, 1, mov(203, 0, null));
        // Entra el relevista 110.
        jugada(1, Mitad.BOTTOM, 204, 110, GROUND_OUT, 0, 1, mov(204, 0, null));
        jugada(1, Mitad.BOTTOM, 205, 110, FLY_OUT, 0, 1, mov(205, 0, null));
        // Alta 2: sacrificio de 106 con carrera de 101... (no en base: solo la carrera sumada).
        jugada(2, Mitad.TOP, 106, 200, SACRIFICE_FLY, 1, 1, mov(106, 0, null), mov(101, 3, 4));
        return CalculadoraEstadisticas.calcular(jugadas, movimientos, Map.of(200L, 25L, 100L, 14L));
    }

    @Test
    void bateo() {
        Calculo c = partido();
        Bateo b101 = c.bateoDe(101);
        assertThat(b101.pa).isEqualTo(1);
        assertThat(b101.h).isEqualTo(1);
        assertThat(b101.sb).isEqualTo(1);
        assertThat(b101.carreras).isEqualTo(2);
        Bateo b102 = c.bateoDe(102);
        assertThat(b102.hr).isEqualTo(1);
        assertThat(b102.rbi).isEqualTo(2);
        assertThat(b102.slg()).isEqualTo(4.0);
        Bateo b104 = c.bateoDe(104);
        assertThat(b104.ab).isZero();
        assertThat(b104.obp()).isEqualTo(1.0);
        // El elevado de sacrificio impulsa pero no es turno oficial.
        Bateo b106 = c.bateoDe(106);
        assertThat(b106.ab).isZero();
        assertThat(b106.rbi).isEqualTo(1);
        assertThat(b106.sf).isEqualTo(1);
        // Embasarse por error no es hit ni impulsa.
        assertThat(c.bateoDe(201).h).isZero();
        assertThat(c.bateoDe(202).rbi).isEqualTo(1);
        assertThat(c.bateoDe(105).ab).isEqualTo(1);
    }

    @Test
    void pitcheo() {
        Calculo c = partido();
        Pitcheo p200 = c.pitcheoDe(200);
        assertThat(p200.outs).isEqualTo(4);
        assertThat(p200.ip()).isEqualTo("1.1");
        assertThat(p200.h).isEqualTo(2);
        assertThat(p200.hr).isEqualTo(1);
        assertThat(p200.carreras).isEqualTo(3);
        assertThat(p200.limpias).isEqualTo(3);
        assertThat(p200.so).isEqualTo(1);
        assertThat(p200.bb).isEqualTo(1);
        assertThat(p200.lanzamientos).isEqualTo(25);
        assertThat(p200.era()).isCloseTo(20.25, within(0.001));
        assertThat(p200.iniciados).containsExactly(1L);

        Pitcheo p100 = c.pitcheoDe(100);
        assertThat(p100.carreras).isEqualTo(1);
        assertThat(p100.limpias).isZero(); // vino de un error
        assertThat(p100.iniciados).containsExactly(1L);
        assertThat(c.pitcheoDe(110).iniciados).isEmpty();
        assertThat(c.pitcheoDe(110).outs).isEqualTo(2);
    }

    @Test
    void ganadorYPerdedor() {
        Calculo c = partido();
        // La visita tomó la ventaja con el jonrón (lanzaba 200, del local): pierde 200.
        assertThat(c.pitcheoDe(200).perdidos).isEqualTo(1);
        // Antes de ese momento la visita no había defendido: gana su abridor 100.
        assertThat(c.pitcheoDe(100).ganados).isEqualTo(1);
        assertThat(c.pitcheoDe(110).ganados).isZero();
    }

    @Test
    void elPronosticoSubeConMejorBateadorYBajaContraMejorLanzador() {
        Pronostico.Liga liga = new Pronostico.Liga(0.250, 0.320, 0.200);
        Bateo bueno = new Bateo();
        bueno.pa = 100;
        bueno.ab = 90;
        bueno.h = 36; // .400
        bueno.bb = 10;
        Bateo promedio = new Bateo();
        promedio.pa = 100;
        promedio.ab = 100;
        promedio.h = 25;
        promedio.so = 20;
        Pitcheo malo = new Pitcheo();
        malo.bateadores = 100;
        malo.abContra = 100;
        malo.h = 35;
        Pitcheo bueno2 = new Pitcheo();
        bueno2.bateadores = 100;
        bueno2.abContra = 100;
        bueno2.h = 15;
        bueno2.so = 40;

        double base = Pronostico.calcular(promedio, null, null, liga).hit();
        assertThat(base).isCloseTo(0.250, within(0.01));
        assertThat(Pronostico.calcular(bueno, malo, null, liga).hit()).isGreaterThan(0.35);
        assertThat(Pronostico.calcular(promedio, bueno2, null, liga).hit()).isLessThan(base);
        assertThat(Pronostico.calcular(promedio, bueno2, null, liga).ponche()).isGreaterThan(0.25);
        // Sin datos de nadie: el promedio de la liga.
        assertThat(Pronostico.calcular(null, null, null, liga).hit()).isCloseTo(0.250, within(0.001));
        // El historial directo empuja el resultado.
        Bateo directo = new Bateo();
        directo.pa = 10;
        directo.ab = 10;
        directo.h = 7;
        assertThat(Pronostico.calcular(promedio, null, directo, liga).hit()).isGreaterThan(0.35);
    }
}
