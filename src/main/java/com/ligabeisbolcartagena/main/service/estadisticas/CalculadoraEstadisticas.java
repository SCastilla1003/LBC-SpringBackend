package com.ligabeisbolcartagena.main.service.estadisticas;

import com.ligabeisbolcartagena.main.dominio.Jugada.Resultado;
import com.ligabeisbolcartagena.main.dominio.Mitad;
import com.ligabeisbolcartagena.main.dominio.Partido;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ligabeisbolcartagena.main.dominio.Jugada.Resultado.*;

/**
 * Estadísticas de bateo y pitcheo calculadas a partir de las jugadas del anotador.
 *
 * Simplificaciones (documentadas en CONTRATO_API.md):
 * - Carreras sucias (no limpias): las que entran en la jugada del error y las de corredores que
 *   llegaron a base por error en esa misma mitad. No se reconstruye la entrada sin el error.
 * - Las carreras se cargan al lanzador que estaba lanzando en la jugada.
 * - Impulsadas: carreras de la jugada salvo doble play o error.
 * - Ganador/perdedor: el perdedor es quien lanzaba cuando el ganador tomó la ventaja definitiva;
 *   el ganador, el último lanzador del equipo ganador antes de esa jugada (o su abridor).
 */
public final class CalculadoraEstadisticas {

    private static final Set<Resultado> DE_CORREDORES =
            EnumSet.of(STOLEN_BASE, CAUGHT_STEALING, WILD_PITCH, PASSED_BALL, BALK);
    private static final Set<Resultado> HITS = EnumSet.of(SINGLE, DOUBLE, TRIPLE, HOME_RUN);
    /** Aparición al plato que no es turno oficial. */
    private static final Set<Resultado> NO_TURNO = EnumSet.of(WALK, HIT_BY_PITCH, SACRIFICE_BUNT, SACRIFICE_FLY);

    /** Una jugada con lo necesario de su partido. */
    public record FilaJugada(long id, long partidoId, int secuencia, int entrada, Mitad mitad, long bateadorId,
                             long lanzadorId, Resultado resultado, int carreras, int outs, long localId,
                             long visitaId, LocalDateTime fecha, Partido.Estado estadoPartido, int carrerasLocal,
                             int carrerasVisita) {
        /** Equipo que batea en la jugada. */
        public long equipoAlBate() {
            return mitad == Mitad.TOP ? visitaId : localId;
        }

        public long equipoEnDefensa() {
            return mitad == Mitad.TOP ? localId : visitaId;
        }

        public boolean esTurno() {
            return !DE_CORREDORES.contains(resultado);
        }
    }

    /** Movimiento de un corredor (o del bateador, desde 0). hasta nulo = out, 4 = anotó. */
    public record FilaMovimiento(long jugadaId, long jugadorId, int desde, Integer hasta) {
    }

    public static final class Bateo {
        public final Set<Long> partidos = new HashSet<>();
        public int pa, ab, h, dobles, triples, hr, carreras, rbi, bb, hbp, so, sb, cs, sf, sh;

        public int juegos() {
            return partidos.size();
        }

        public int basesTotales() {
            return h + dobles + 2 * triples + 3 * hr;
        }

        public double avg() {
            return ab == 0 ? 0 : (double) h / ab;
        }

        public double obp() {
            int d = ab + bb + hbp + sf;
            return d == 0 ? 0 : (double) (h + bb + hbp) / d;
        }

        public double slg() {
            return ab == 0 ? 0 : (double) basesTotales() / ab;
        }

        public double ops() {
            return obp() + slg();
        }

        void sumarTurno(FilaJugada j) {
            partidos.add(j.partidoId());
            pa++;
            Resultado r = j.resultado();
            if (!NO_TURNO.contains(r)) {
                ab++;
            }
            switch (r) {
                case SINGLE -> h++;
                case DOUBLE -> { h++; dobles++; }
                case TRIPLE -> { h++; triples++; }
                case HOME_RUN -> { h++; hr++; }
                case WALK -> bb++;
                case HIT_BY_PITCH -> hbp++;
                case STRIKEOUT -> so++;
                case SACRIFICE_FLY -> sf++;
                case SACRIFICE_BUNT -> sh++;
                default -> { }
            }
            if (r != DOUBLE_PLAY && r != REACHED_ON_ERROR) {
                rbi += j.carreras();
            }
        }
    }

    public static final class Pitcheo {
        public final Set<Long> partidos = new HashSet<>();
        public final Set<Long> iniciados = new HashSet<>();
        public int outs, bateadores, abContra, h, carreras, limpias, bb, hbp, so, hr, lanzamientos, ganados, perdidos;

        public int juegos() {
            return partidos.size();
        }

        /** Entradas lanzadas en formato de béisbol: 5.2 = 5 entradas y 2 outs. */
        public String ip() {
            return outs / 3 + "." + outs % 3;
        }

        public double entradas() {
            return outs / 3.0;
        }

        public double era() {
            return outs == 0 ? 0 : limpias * 9.0 / entradas();
        }

        public double whip() {
            return outs == 0 ? 0 : (bb + h) / entradas();
        }

        public double k9() {
            return outs == 0 ? 0 : so * 9.0 / entradas();
        }

        public double bb9() {
            return outs == 0 ? 0 : bb * 9.0 / entradas();
        }

        /** Promedio de bateo de los rivales. */
        public double promedioContra() {
            return abContra == 0 ? 0 : (double) h / abContra;
        }

        void sumar(FilaJugada j) {
            partidos.add(j.partidoId());
            outs += j.outs();
            carreras += j.carreras();
            if (j.resultado() != REACHED_ON_ERROR) {
                limpias += j.carreras();
            }
            if (!j.esTurno()) {
                return;
            }
            bateadores++;
            Resultado r = j.resultado();
            if (!NO_TURNO.contains(r)) {
                abContra++;
            }
            switch (r) {
                case SINGLE, DOUBLE, TRIPLE -> h++;
                case HOME_RUN -> { h++; hr++; }
                case WALK -> bb++;
                case HIT_BY_PITCH -> hbp++;
                case STRIKEOUT -> so++;
                default -> { }
            }
        }
    }

    public record Calculo(Map<Long, Bateo> bateo, Map<Long, Pitcheo> pitcheo) {
        public Bateo bateoDe(long jugador) {
            return bateo.get(jugador);
        }

        public Pitcheo pitcheoDe(long jugador) {
            return pitcheo.get(jugador);
        }
    }

    private CalculadoraEstadisticas() {
    }

    /** [lanzamientos]: lanzamientos registrados por cada lanzador (puede ser vacío). */
    public static Calculo calcular(List<FilaJugada> jugadas, List<FilaMovimiento> movimientos,
                                   Map<Long, Long> lanzamientos) {
        Map<Long, Bateo> bateo = new LinkedHashMap<>();
        Map<Long, Pitcheo> pitcheo = new LinkedHashMap<>();
        Map<Long, FilaJugada> porId = new HashMap<>();

        for (FilaJugada j : jugadas) {
            porId.put(j.id(), j);
            if (j.esTurno()) {
                bateo.computeIfAbsent(j.bateadorId(), k -> new Bateo()).sumarTurno(j);
            }
            pitcheo.computeIfAbsent(j.lanzadorId(), k -> new Pitcheo()).sumar(j);
        }

        Map<Long, List<FilaMovimiento>> movimientosPorJugada = new HashMap<>();
        for (FilaMovimiento m : movimientos) {
            movimientosPorJugada.computeIfAbsent(m.jugadaId(), k -> new ArrayList<>()).add(m);
            FilaJugada j = porId.get(m.jugadaId());
            if (j == null) {
                continue;
            }
            Bateo b = bateo.computeIfAbsent(m.jugadorId(), k -> new Bateo());
            b.partidos.add(j.partidoId());
            if (m.hasta() != null && m.hasta() == 4 && j.carreras() > 0) {
                b.carreras++;
            }
            if (m.desde() > 0 && j.resultado() == STOLEN_BASE && m.hasta() != null) {
                b.sb++;
            }
            if (m.desde() > 0 && j.resultado() == CAUGHT_STEALING && m.hasta() == null) {
                b.cs++;
            }
        }

        // Abridores y decisiones (ganó / perdió), partido por partido.
        Map<Long, List<FilaJugada>> porPartido = new HashMap<>();
        for (FilaJugada j : jugadas) {
            porPartido.computeIfAbsent(j.partidoId(), k -> new ArrayList<>()).add(j);
        }
        for (List<FilaJugada> l : porPartido.values()) {
            l.sort(Comparator.comparingInt(FilaJugada::secuencia));
            Set<Long> conAbridor = new HashSet<>();
            for (FilaJugada j : l) {
                if (conAbridor.add(j.equipoEnDefensa())) {
                    pitcheo.computeIfAbsent(j.lanzadorId(), k -> new Pitcheo()).iniciados.add(j.partidoId());
                }
            }
            decisiones(l, pitcheo);
            carrerasSucias(l, movimientosPorJugada, pitcheo);
        }

        lanzamientos.forEach((id, n) -> {
            Pitcheo p = pitcheo.get(id);
            if (p != null) {
                p.lanzamientos += n.intValue();
            }
        });
        return new Calculo(bateo, pitcheo);
    }

    /**
     * Las carreras de corredores que llegaron a base por error en esa misma mitad no son limpias.
     * (Las que entran en la propia jugada del error ya se restaron al sumarla.)
     */
    private static void carrerasSucias(List<FilaJugada> l, Map<Long, List<FilaMovimiento>> movimientos,
                                       Map<Long, Pitcheo> pitcheo) {
        Set<Long> porError = new HashSet<>();
        String mitad = null;
        for (FilaJugada j : l) {
            String esta = j.entrada() + "-" + j.mitad();
            if (!esta.equals(mitad)) {
                porError.clear();
                mitad = esta;
            }
            if (j.resultado() == REACHED_ON_ERROR) {
                porError.add(j.bateadorId());
                continue;
            }
            if (j.carreras() == 0) {
                continue;
            }
            for (FilaMovimiento m : movimientos.getOrDefault(j.id(), List.of())) {
                if (m.hasta() != null && m.hasta() == 4 && porError.contains(m.jugadorId())) {
                    pitcheo.get(j.lanzadorId()).limpias--;
                }
            }
        }
    }

    private static void decisiones(List<FilaJugada> l, Map<Long, Pitcheo> pitcheo) {
        FilaJugada primera = l.get(0);
        if (primera.estadoPartido() != Partido.Estado.FINISHED
                || primera.carrerasLocal() == primera.carrerasVisita()) {
            return;
        }
        long ganador = primera.carrerasLocal() > primera.carrerasVisita() ? primera.localId() : primera.visitaId();
        int favor = 0;
        int contra = 0;
        int ventaja = -1; // índice de la jugada que dio la ventaja definitiva
        for (int i = 0; i < l.size(); i++) {
            FilaJugada j = l.get(i);
            boolean anotaGanador = j.equipoAlBate() == ganador;
            boolean ibaArriba = favor > contra;
            if (anotaGanador) {
                favor += j.carreras();
            } else {
                contra += j.carreras();
            }
            boolean vaArriba = favor > contra;
            if (vaArriba && !ibaArriba) {
                ventaja = i;
            } else if (!vaArriba) {
                ventaja = -1;
            }
        }
        if (ventaja < 0) {
            return; // las carreras del partido no cuadran con las jugadas (p. ej. se finalizó a mano)
        }
        FilaJugada clave = l.get(ventaja);
        Long lanzadorGanador = null;
        for (int i = ventaja - 1; i >= 0 && lanzadorGanador == null; i--) {
            if (l.get(i).equipoEnDefensa() == ganador) {
                lanzadorGanador = l.get(i).lanzadorId();
            }
        }
        if (lanzadorGanador == null) {
            lanzadorGanador = l.stream().filter(j -> j.equipoEnDefensa() == ganador).map(FilaJugada::lanzadorId)
                    .findFirst().orElse(null);
        }
        if (lanzadorGanador != null) {
            pitcheo.computeIfAbsent(lanzadorGanador, k -> new Pitcheo()).ganados++;
        }
        pitcheo.computeIfAbsent(clave.lanzadorId(), k -> new Pitcheo()).perdidos++;
    }
}
