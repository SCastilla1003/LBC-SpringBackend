package com.ligabeisbolcartagena.main.service.juego;

import com.ligabeisbolcartagena.main.dominio.PosicionAlineacion;
import com.ligabeisbolcartagena.main.dominio.Sustitucion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Quién está jugando ahora en cada equipo: la alineación inicial con los cambios aplicados
 * en orden. Un jugador que sale ya no puede volver (regla del béisbol).
 */
public final class AlineacionActual implements MotorJuego.Alineaciones {

    /** Turno del orden de bateo: jugador y posición (nulos si la alineación está incompleta). */
    public record Turno(int orden, Long jugadorId, String posicion) {
    }

    private final Equipo local = new Equipo();
    private final Equipo visita = new Equipo();

    private static final class Equipo {
        final Turno[] orden = new Turno[10];
        Long lanzador;
        final Set<Long> retirados = new HashSet<>();

        Equipo() {
            for (int i = 1; i <= 9; i++) {
                orden[i] = new Turno(i, null, null);
            }
        }
    }

    /** [idLocal]: id del equipo local, para repartir las filas de la alineación. */
    public static AlineacionActual construir(Long idLocal, List<PosicionAlineacion> filas,
                                             List<Sustitucion> cambios) {
        AlineacionActual a = new AlineacionActual();
        for (PosicionAlineacion f : filas) {
            Equipo e = f.getEquipo().getId().equals(idLocal) ? a.local : a.visita;
            Integer orden = f.getOrdenBateo();
            if (orden != null && orden >= 1 && orden <= 9) {
                e.orden[orden] = new Turno(orden, f.getJugador().getId(), f.getPosicion());
            }
            if (f.isLanzadorInicial() || "P".equals(f.getPosicion())) {
                e.lanzador = f.getJugador().getId();
            }
        }
        for (Sustitucion s : cambios) {
            a.aplicar(s.getEquipo().getId().equals(idLocal), s.getTipo(), s.getJugadorSale().getId(),
                    s.getJugadorEntra().getId(), s.getPosicion());
        }
        return a;
    }

    /** Aplica un cambio ya validado. */
    public void aplicar(boolean deLocal, Sustitucion.Tipo tipo, Long sale, Long entra, String posicion) {
        Equipo e = deLocal ? local : visita;
        Integer orden = ordenDe(deLocal, sale);
        if (sale.equals(entra)) {
            // Solo cambia de posición en el terreno.
            if (orden != null) {
                e.orden[orden] = new Turno(orden, entra, posicion);
            }
            return;
        }
        if (tipo == Sustitucion.Tipo.PITCHER && orden != null && "DH".equals(e.orden[orden].posicion())) {
            // Regla Ohtani: deja de lanzar pero sigue bateando como DH.
            e.lanzador = entra;
            return;
        }
        e.retirados.add(sale);
        if (tipo == Sustitucion.Tipo.PITCHER) {
            e.lanzador = entra;
            if (orden != null) {
                e.orden[orden] = new Turno(orden, entra, "P");
            }
            return;
        }
        if (orden != null) {
            String pos = posicion != null ? posicion
                    : switch (tipo) {
                        case PINCH_HITTER -> "PH";
                        case PINCH_RUNNER -> "PR";
                        default -> e.orden[orden].posicion();
                    };
            e.orden[orden] = new Turno(orden, entra, pos);
        }
    }

    @Override
    public Long bateadorEn(boolean local, int orden) {
        return (local ? this.local : visita).orden[orden].jugadorId();
    }

    @Override
    public Long lanzadorDe(boolean local) {
        return (local ? this.local : visita).lanzador;
    }

    public List<Turno> orden(boolean local) {
        Equipo e = local ? this.local : visita;
        List<Turno> l = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            l.add(e.orden[i]);
        }
        return l;
    }

    public Integer ordenDe(boolean local, Long jugador) {
        Equipo e = local ? this.local : visita;
        for (int i = 1; i <= 9; i++) {
            if (jugador != null && jugador.equals(e.orden[i].jugadorId())) {
                return i;
            }
        }
        return null;
    }

    /** Está en el orden de bateo o es el lanzador del equipo. */
    public boolean enJuego(boolean local, Long jugador) {
        return ordenDe(local, jugador) != null || (jugador != null && jugador.equals(lanzadorDe(local)));
    }

    public boolean retirado(boolean local, Long jugador) {
        return (local ? this.local : visita).retirados.contains(jugador);
    }

    /** 9 bateadores y lanzador en ambos equipos. */
    public List<String> faltantes(String nombreLocal, String nombreVisita) {
        List<String> faltan = new ArrayList<>();
        for (boolean esLocal : new boolean[] {false, true}) {
            Equipo e = esLocal ? local : visita;
            String nombre = esLocal ? nombreLocal : nombreVisita;
            long bateadores = orden(esLocal).stream().filter(t -> t.jugadorId() != null).count();
            if (bateadores < 9) {
                faltan.add(nombre + ": " + bateadores + " de 9 bateadores");
            }
            if (e.lanzador == null) {
                faltan.add(nombre + ": falta el lanzador inicial");
            }
        }
        return faltan;
    }
}
