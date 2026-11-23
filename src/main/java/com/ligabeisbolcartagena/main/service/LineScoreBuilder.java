package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.PartidoDtos.EntradaLineScoreDto;
import com.ligabeisbolcartagena.main.api.dto.PartidoDtos.LineScoreDto;
import com.ligabeisbolcartagena.main.dominio.Mitad;
import com.ligabeisbolcartagena.main.dominio.Partido;

import java.util.ArrayList;
import java.util.List;

/**
 * Marcador por entrada, como en MLB.
 * - Programado: tantas casillas vacías como entradas programadas.
 * - En vivo: las programadas (o más si el partido va en extra innings), con las entradas sin jugar en null.
 * - Finalizado: solo las entradas jugadas (un partido de 7 entradas muestra 7).
 *   Si pasa de las programadas, entradasExtra = true para que la app lo avise.
 * La columna visita corresponde a la mitad TOP y la local a la BOTTOM.
 */
public final class LineScoreBuilder {

    public record CarreraMitad(int entrada, Mitad mitad, int carreras) {
    }

    private LineScoreBuilder() {
    }

    public static LineScoreDto construir(int entradasProgramadas, Partido.Estado estado, int entradaActual,
                                         Mitad mitadActual, List<CarreraMitad> carreras) {
        int ultimaJugada = carreras.stream().mapToInt(CarreraMitad::entrada).max().orElse(0);
        boolean enCurso = estado == Partido.Estado.LIVE || estado == Partido.Estado.SUSPENDED;
        boolean terminado = estado == Partido.Estado.FINISHED;

        int total;
        if (terminado) {
            total = Math.max(ultimaJugada, entradaActual);
        } else {
            total = Math.max(entradasProgramadas, enCurso ? entradaActual : 0);
        }
        total = Math.max(total, 1);

        List<EntradaLineScoreDto> entradas = new ArrayList<>();
        for (int n = 1; n <= total; n++) {
            Integer visita = alcanzada(n, Mitad.TOP, terminado, enCurso, entradaActual, mitadActual)
                    ? sumar(carreras, n, Mitad.TOP) : null;
            Integer local = alcanzada(n, Mitad.BOTTOM, terminado, enCurso, entradaActual, mitadActual)
                    ? sumar(carreras, n, Mitad.BOTTOM) : null;
            entradas.add(new EntradaLineScoreDto(n, local, visita));
        }
        return new LineScoreDto(entradasProgramadas, total > entradasProgramadas, entradas);
    }

    private static boolean alcanzada(int n, Mitad mitad, boolean terminado, boolean enCurso, int entradaActual,
                                     Mitad mitadActual) {
        if (terminado) {
            return true;
        }
        if (!enCurso) {
            return false;
        }
        if (n < entradaActual) {
            return true;
        }
        if (n > entradaActual) {
            return false;
        }
        return mitad == Mitad.TOP || mitadActual == Mitad.BOTTOM;
    }

    private static int sumar(List<CarreraMitad> carreras, int entrada, Mitad mitad) {
        return carreras.stream()
                .filter(c -> c.entrada() == entrada && c.mitad() == mitad)
                .mapToInt(CarreraMitad::carreras)
                .sum();
    }
}
