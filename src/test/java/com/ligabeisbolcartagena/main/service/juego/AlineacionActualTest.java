package com.ligabeisbolcartagena.main.service.juego;

import com.ligabeisbolcartagena.main.dominio.Equipo;
import com.ligabeisbolcartagena.main.dominio.Jugador;
import com.ligabeisbolcartagena.main.dominio.PosicionAlineacion;
import com.ligabeisbolcartagena.main.dominio.Sustitucion;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlineacionActualTest {

    static Equipo equipo(long id) {
        Equipo e = new Equipo();
        e.setId(id);
        return e;
    }

    static Jugador jugador(long id) {
        Jugador j = new Jugador();
        j.setId(id);
        return j;
    }

    static PosicionAlineacion fila(Equipo e, long jugador, Integer orden, String posicion) {
        PosicionAlineacion f = new PosicionAlineacion();
        f.setEquipo(e);
        f.setJugador(jugador(jugador));
        f.setOrdenBateo(orden);
        f.setPosicion(posicion);
        return f;
    }

    /** Local (id 1): 201..209 y lanza 200. Visita (id 2): solo 5 bateadores y sin lanzador. */
    static List<PosicionAlineacion> filas() {
        Equipo local = equipo(1);
        Equipo visita = equipo(2);
        List<PosicionAlineacion> l = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            l.add(fila(local, 200 + i, i, i == 9 ? "DH" : "SS"));
        }
        l.add(fila(local, 200, null, "P"));
        for (int i = 1; i <= 5; i++) {
            l.add(fila(visita, 100 + i, i, "C"));
        }
        return l;
    }

    static Sustitucion cambio(Sustitucion.Tipo tipo, long sale, long entra) {
        Sustitucion s = new Sustitucion();
        s.setEquipo(equipo(1));
        s.setTipo(tipo);
        s.setJugadorSale(jugador(sale));
        s.setJugadorEntra(jugador(entra));
        return s;
    }

    @Test
    void indicaQueFaltaParaIniciar() {
        AlineacionActual a = AlineacionActual.construir(1L, filas(), List.of());
        assertThat(a.faltantes("Tigres", "Leones"))
                .containsExactly("Leones: 5 de 9 bateadores", "Leones: falta el lanzador inicial");
        assertThat(a.lanzadorDe(true)).isEqualTo(200L);
        assertThat(a.bateadorEn(true, 3)).isEqualTo(203L);
    }

    @Test
    void losCambiosOcupanElTurnoYQuienSaleNoVuelve() {
        AlineacionActual a = AlineacionActual.construir(1L, filas(), List.of(
                cambio(Sustitucion.Tipo.PINCH_HITTER, 203, 250),
                cambio(Sustitucion.Tipo.PITCHER, 200, 260)));
        assertThat(a.bateadorEn(true, 3)).isEqualTo(250L);
        assertThat(a.orden(true).get(2).posicion()).isEqualTo("PH");
        assertThat(a.lanzadorDe(true)).isEqualTo(260L);
        assertThat(a.retirado(true, 203L)).isTrue();
        assertThat(a.retirado(true, 200L)).isTrue();
        assertThat(a.enJuego(true, 260L)).isTrue();
        assertThat(a.enJuego(true, 203L)).isFalse();
    }
}
