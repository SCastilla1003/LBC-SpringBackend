package com.ligabeisbolcartagena.main.service.juego;

import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.dominio.Jugada.Resultado;
import com.ligabeisbolcartagena.main.dominio.Lanzamiento;
import com.ligabeisbolcartagena.main.dominio.Mitad;
import com.ligabeisbolcartagena.main.dominio.Partido;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.ligabeisbolcartagena.main.dominio.Jugada.Resultado.*;

/**
 * Reglas del juego para el anotador, sin base de datos: conteo, outs, corredores, carreras,
 * cambio de mitad y fin del partido. El anotador decide a dónde va cada corredor; si no lo
 * dice, se usa el avance típico de cada jugada ({@link #porDefecto}).
 *
 * Simplificación: si la jugada hace el tercer out y ese out es del bateador o es forzado
 * (rolata, bola ocupada, doble play, atrapado robando), las carreras de la jugada no cuentan.
 */
public final class MotorJuego {

    /** Jugadas de corredores: no cierran el turno del bateador. */
    public static final Set<Resultado> DE_CORREDORES =
            EnumSet.of(STOLEN_BASE, CAUGHT_STEALING, WILD_PITCH, PASSED_BALL, BALK);

    public static final Set<Resultado> HITS = EnumSet.of(SINGLE, DOUBLE, TRIPLE, HOME_RUN);

    /** El bateador llega a base sí o sí. */
    private static final Set<Resultado> BATEADOR_A_SALVO =
            EnumSet.of(SINGLE, DOUBLE, TRIPLE, HOME_RUN, WALK, HIT_BY_PITCH, REACHED_ON_ERROR, FIELDERS_CHOICE);

    /** Si hacen el tercer out, sus carreras no cuentan. */
    private static final Set<Resultado> OUT_FORZADO =
            EnumSet.of(GROUND_OUT, FIELDERS_CHOICE, DOUBLE_PLAY, CAUGHT_STEALING, STRIKEOUT);

    /** Bolas bateadas (y el pelotazo): suman un lanzamiento "en juego". */
    public static final Set<Resultado> CON_LANZAMIENTO_EN_JUEGO = EnumSet.of(SINGLE, DOUBLE, TRIPLE, HOME_RUN,
            HIT_BY_PITCH, GROUND_OUT, FLY_OUT, LINE_OUT, POP_OUT, FIELDERS_CHOICE, REACHED_ON_ERROR,
            SACRIFICE_BUNT, SACRIFICE_FLY, DOUBLE_PLAY);

    /** Quién batea en cada turno y quién lanza por cada equipo en este momento. */
    public interface Alineaciones {
        Long bateadorEn(boolean local, int orden);

        Long lanzadorDe(boolean local);
    }

    /** Lo que pasó en una jugada ya aplicada. */
    public record Resumen(List<Movimiento> movimientos, Map<Integer, Long> corredores, int carreras, int outs,
                          List<Long> anotaron, boolean cambioMitad, boolean finPartido) {
    }

    private MotorJuego() {
    }

    // ------------------------------------------------------------------ inicio

    public static void iniciar(EstadoVivo s, Alineaciones a) {
        s.estado = Partido.Estado.LIVE;
        s.entrada = 1;
        s.mitad = Mitad.TOP;
        s.outs = 0;
        s.bolas = 0;
        s.strikes = 0;
        s.limpiarBases();
        s.proximoLocal = 1;
        s.proximoVisita = 1;
        s.carrerasLocal = 0;
        s.carrerasVisita = 0;
        s.hitsLocal = 0;
        s.hitsVisita = 0;
        s.erroresLocal = 0;
        s.erroresVisita = 0;
        s.bateador = a.bateadorEn(false, 1);
        s.lanzador = a.lanzadorDe(true);
    }

    // ------------------------------------------------------------------ lanzamientos

    /**
     * Aplica un lanzamiento al conteo. Si completa la base por bolas o el ponche, no toca el
     * conteo y devuelve la jugada que hay que aplicar (WALK o STRIKEOUT).
     */
    public static Optional<Resultado> lanzamiento(EstadoVivo s, Lanzamiento.Tipo tipo) {
        exigirEnVivo(s);
        switch (tipo) {
            case BALL -> {
                if (s.bolas == 3) {
                    return Optional.of(WALK);
                }
                s.bolas++;
            }
            case CALLED_STRIKE, SWINGING_STRIKE -> {
                if (s.strikes == 2) {
                    return Optional.of(STRIKEOUT);
                }
                s.strikes++;
            }
            case FOUL -> {
                if (s.strikes < 2) {
                    s.strikes++;
                }
            }
            case IN_PLAY -> throw ApiException.invalida("Para una bola en juego registra el resultado de la jugada");
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------ jugadas

    /** Avance típico de cada jugada según los corredores que hay en base. */
    public static List<Movimiento> porDefecto(EstadoVivo s, Resultado r) {
        Long[] b = s.bases;
        List<Movimiento> m = new ArrayList<>();
        switch (r) {
            case SINGLE, REACHED_ON_ERROR -> {
                m.add(Movimiento.a(0, 1));
                todosAvanzan(b, 1, m);
            }
            case DOUBLE -> {
                m.add(Movimiento.a(0, 2));
                todosAvanzan(b, 2, m);
            }
            case TRIPLE -> {
                m.add(Movimiento.a(0, 3));
                todosAvanzan(b, 3, m);
            }
            case HOME_RUN -> {
                m.add(Movimiento.a(0, 4));
                todosAvanzan(b, 4, m);
            }
            case WALK, HIT_BY_PITCH -> {
                m.add(Movimiento.a(0, 1));
                forzados(b, m);
            }
            case STRIKEOUT, FLY_OUT, LINE_OUT, POP_OUT, GROUND_OUT -> m.add(Movimiento.out(0));
            case SACRIFICE_FLY -> {
                m.add(Movimiento.out(0));
                if (b[3] != null) {
                    m.add(Movimiento.a(3, 4));
                }
            }
            case SACRIFICE_BUNT -> {
                m.add(Movimiento.out(0));
                todosAvanzan(b, 1, m);
            }
            case FIELDERS_CHOICE -> {
                m.add(Movimiento.a(0, 1));
                if (b[1] != null) {
                    m.add(Movimiento.out(1));
                    if (b[2] != null) {
                        m.add(Movimiento.a(2, 3));
                        if (b[3] != null) {
                            m.add(Movimiento.a(3, 4));
                        }
                    }
                } else {
                    int lider = corredorLider(b);
                    if (lider > 0) {
                        m.add(Movimiento.out(lider));
                    }
                }
            }
            case DOUBLE_PLAY -> {
                m.add(Movimiento.out(0));
                int primero = b[1] != null ? 1 : corredorLider(b);
                if (primero > 0) {
                    m.add(Movimiento.out(primero));
                }
            }
            case STOLEN_BASE, CAUGHT_STEALING -> {
                int quien = corredorQueRoba(b);
                if (quien > 0) {
                    m.add(r == STOLEN_BASE ? Movimiento.a(quien, quien + 1) : Movimiento.out(quien));
                }
            }
            case WILD_PITCH, PASSED_BALL, BALK -> todosAvanzan(b, 1, m);
        }
        return m;
    }

    /**
     * Valida y aplica una jugada. [movimientos] nulo = avance típico. Los corredores que no
     * aparecen se quedan en su base. [errores] son los errores de la defensa en la jugada.
     */
    public static Resumen aplicarJugada(EstadoVivo s, Resultado r, List<Movimiento> movimientos, int errores,
                                        Alineaciones a) {
        exigirEnVivo(s);
        boolean turno = !DE_CORREDORES.contains(r);
        List<Movimiento> movs = movimientos == null ? porDefecto(s, r) : movimientos;

        Map<Integer, Movimiento> porDesde = new HashMap<>();
        for (Movimiento m : movs) {
            if (m.desde() < 0 || m.desde() > 3) {
                throw ApiException.invalida("Base de origen no válida: " + m.desde());
            }
            if (m.desde() == 0 && !turno) {
                throw ApiException.invalida("En esta jugada el bateador no se mueve");
            }
            if (m.desde() > 0 && s.bases[m.desde()] == null) {
                throw ApiException.invalida("No hay corredor en " + nombreBase(m.desde()));
            }
            if (porDesde.put(m.desde(), m) != null) {
                throw ApiException.invalida("Hay dos movimientos para el mismo corredor");
            }
            if (m.hasta() != null && (m.hasta() < 1 || m.hasta() > 4 || m.hasta() < m.desde())) {
                throw ApiException.invalida("Un corredor no puede retroceder ni ir a una base que no existe");
            }
        }
        if (turno && !porDesde.containsKey(0)) {
            throw ApiException.invalida("Falta indicar qué pasó con el bateador");
        }
        if (!turno && porDesde.isEmpty()) {
            throw ApiException.invalida("Indica qué corredor se movió");
        }
        Movimiento bateador = porDesde.get(0);
        if (bateador != null) {
            if (BATEADOR_A_SALVO.contains(r) && bateador.esOut()) {
                throw ApiException.invalida("En esta jugada el bateador llega a base");
            }
            int minimo = switch (r) {
                case DOUBLE -> 2;
                case TRIPLE -> 3;
                case HOME_RUN -> 4;
                default -> 1;
            };
            if (!bateador.esOut() && bateador.hasta() < minimo) {
                throw ApiException.invalida("En esta jugada el bateador llega por lo menos a " + nombreBase(minimo));
            }
        }

        // Situación final de cada corredor (incluido el bateador): base 1..3, 4 = anotó, -1 = out.
        Map<Integer, Integer> destino = new HashMap<>();
        if (turno) {
            destino.put(0, bateador.esOut() ? -1 : bateador.hasta());
        }
        for (int base = 1; base <= 3; base++) {
            if (s.bases[base] != null) {
                Movimiento m = porDesde.get(base);
                destino.put(base, m == null ? base : (m.esOut() ? -1 : m.hasta()));
            }
        }
        Long[] nuevas = new Long[4];
        List<Long> anotaron = new ArrayList<>();
        int outs = 0;
        for (Map.Entry<Integer, Integer> e : destino.entrySet()) {
            Long jugador = e.getKey() == 0 ? s.bateador : s.bases[e.getKey()];
            int d = e.getValue();
            if (d == -1) {
                outs++;
            } else if (d == 4) {
                anotaron.add(jugador);
            } else {
                if (nuevas[d] != null) {
                    throw ApiException.invalida("Dos corredores no pueden quedar en " + nombreBase(d));
                }
                nuevas[d] = jugador;
            }
        }
        // Nadie adelanta a un corredor que iba delante de él.
        for (Map.Entry<Integer, Integer> x : destino.entrySet()) {
            for (Map.Entry<Integer, Integer> y : destino.entrySet()) {
                if (x.getKey() < y.getKey() && x.getValue() > 0 && y.getValue() > 0 && x.getValue() > y.getValue()) {
                    throw ApiException.invalida("Un corredor no puede adelantar al que va delante");
                }
            }
        }
        if (r == DOUBLE_PLAY && outs < 2) {
            throw ApiException.invalida("Un doble play necesita dos outs");
        }
        if (r == CAUGHT_STEALING && outs < 1) {
            throw ApiException.invalida("Atrapado robando necesita un out");
        }
        if (s.outs + outs > 3) {
            throw ApiException.invalida("No puede haber más de 3 outs en la entrada");
        }
        if (errores < 0 || errores > 4) {
            throw ApiException.invalida("Errores no válidos");
        }

        boolean local = s.bateaLocal();
        boolean tercerOut = s.outs + outs == 3;
        boolean bateadorOut = bateador != null && bateador.esOut();
        int carreras = tercerOut && (bateadorOut || OUT_FORZADO.contains(r)) ? 0 : anotaron.size();
        if (carreras == 0) {
            anotaron = List.of();
        }

        Map<Integer, Long> corredores = new HashMap<>();
        for (int base = 1; base <= 3; base++) {
            if (s.bases[base] != null) {
                corredores.put(base, s.bases[base]);
            }
        }
        if (turno) {
            corredores.put(0, s.bateador);
        }

        s.bases = nuevas;
        s.sumarCarreras(local, carreras);
        if (HITS.contains(r)) {
            s.sumarHit(local);
        }
        int erroresJugada = r == REACHED_ON_ERROR ? Math.max(errores, 1) : errores;
        s.sumarErrores(!local, erroresJugada);
        s.outs += outs;
        if (turno) {
            s.bolas = 0;
            s.strikes = 0;
            s.avanzarOrden(local);
        }

        boolean cambio = false;
        boolean fin = false;
        if (local && s.entrada >= s.entradasProgramadas && s.carrerasLocal > s.carrerasVisita) {
            terminar(s); // el local deja en el terreno al visitante
            fin = true;
        } else if (s.outs >= 3) {
            fin = !cambiarMitad(s, a);
            cambio = !fin;
        } else if (turno) {
            s.bateador = a.bateadorEn(local, s.proximo(local));
        }
        return new Resumen(movs, corredores, carreras, outs, anotaron, cambio, fin);
    }

    /**
     * Cierra la mitad actual. Devuelve false si con eso termina el partido (el local ya ganó
     * sin batear la baja, o una entrada completa de las finales deja a alguien arriba).
     */
    public static boolean cambiarMitad(EstadoVivo s, Alineaciones a) {
        if (s.entrada >= s.entradasProgramadas) {
            boolean ganaLocal = s.carrerasLocal > s.carrerasVisita;
            if ((s.mitad == Mitad.TOP && ganaLocal)
                    || (s.mitad == Mitad.BOTTOM && s.carrerasLocal != s.carrerasVisita)) {
                terminar(s);
                return false;
            }
        }
        s.outs = 0;
        s.bolas = 0;
        s.strikes = 0;
        s.limpiarBases();
        if (s.mitad == Mitad.TOP) {
            s.mitad = Mitad.BOTTOM;
        } else {
            s.mitad = Mitad.TOP;
            s.entrada++;
        }
        boolean local = s.bateaLocal();
        s.bateador = a.bateadorEn(local, s.proximo(local));
        s.lanzador = a.lanzadorDe(!local);
        return true;
    }

    /** Final del partido: deja la situación limpia (la tabla no admite 3 outs). */
    public static void terminar(EstadoVivo s) {
        s.estado = Partido.Estado.FINISHED;
        s.outs = 0;
        s.bolas = 0;
        s.strikes = 0;
        s.limpiarBases();
        s.bateador = null;
    }

    public static String nombreBase(int base) {
        return switch (base) {
            case 1 -> "primera";
            case 2 -> "segunda";
            case 3 -> "tercera";
            case 4 -> "home";
            default -> "home";
        };
    }

    // ------------------------------------------------------------------ ayudas

    private static void exigirEnVivo(EstadoVivo s) {
        if (s.estado != Partido.Estado.LIVE) {
            throw ApiException.conflicto("El partido no está en vivo");
        }
    }

    /** Cada corredor avanza [n] bases (4 o más = anota), de tercera hacia atrás. */
    private static void todosAvanzan(Long[] b, int n, List<Movimiento> m) {
        for (int base = 3; base >= 1; base--) {
            if (b[base] != null) {
                m.add(Movimiento.a(base, Math.min(4, base + n)));
            }
        }
    }

    /** Solo avanzan los corredores obligados por el bateador que llega a primera. */
    private static void forzados(Long[] b, List<Movimiento> m) {
        if (b[1] == null) {
            return;
        }
        m.add(Movimiento.a(1, 2));
        if (b[2] == null) {
            return;
        }
        m.add(Movimiento.a(2, 3));
        if (b[3] != null) {
            m.add(Movimiento.a(3, 4));
        }
    }

    private static int corredorLider(Long[] b) {
        for (int base = 3; base >= 1; base--) {
            if (b[base] != null) {
                return base;
            }
        }
        return 0;
    }

    /** El corredor más adelantado que tiene libre la base siguiente (o el de tercera, que roba home). */
    private static int corredorQueRoba(Long[] b) {
        if (b[2] != null && b[3] == null) {
            return 2;
        }
        if (b[1] != null && b[2] == null) {
            return 1;
        }
        return corredorLider(b);
    }
}
