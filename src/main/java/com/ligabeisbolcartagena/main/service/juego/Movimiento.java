package com.ligabeisbolcartagena.main.service.juego;

/**
 * Lo que pasó con un corredor (o con el bateador, desde = 0) en una jugada.
 * hasta: 1..3 = base, 4 = anotó; nulo = fue out.
 */
public record Movimiento(int desde, Integer hasta) {

    public static Movimiento a(int desde, int hasta) {
        return new Movimiento(desde, hasta);
    }

    public static Movimiento out(int desde) {
        return new Movimiento(desde, null);
    }

    public boolean esOut() {
        return hasta == null;
    }

    public boolean anota() {
        return hasta != null && hasta == 4;
    }
}
