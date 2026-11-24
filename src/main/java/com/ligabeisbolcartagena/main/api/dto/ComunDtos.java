package com.ligabeisbolcartagena.main.api.dto;

import com.ligabeisbolcartagena.main.dominio.Jugador;

/** Piezas reutilizadas por varios recursos (equipos, jugadores y referencias). */
public final class ComunDtos {

    /** Posiciones defensivas admitidas en la plantilla y en la alineación. */
    public static final String POSICION_REGEX = "^(P|C|1B|2B|3B|SS|LF|CF|RF|DH)$";

    private ComunDtos() {
    }

    public record EquipoResumen(Long id, String nombre, String abreviatura, String color, String escudoUrl) {
    }

    public record JugadorResumen(Long id, String nombres, String apellidos, Jugador.LadoBateo batea,
                                 Jugador.LadoLanzamiento lanza, String fotoUrl, boolean esLanzador) {
    }

    public record JugadorRef(Long id, String nombre) {
    }
}
