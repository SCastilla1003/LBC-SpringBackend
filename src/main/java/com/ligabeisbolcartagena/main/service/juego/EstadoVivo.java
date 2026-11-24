package com.ligabeisbolcartagena.main.service.juego;

import com.ligabeisbolcartagena.main.dominio.Jugador;
import com.ligabeisbolcartagena.main.dominio.Mitad;
import com.ligabeisbolcartagena.main.dominio.Partido;

import java.util.function.Function;

/**
 * Situación en vivo de un partido (lo que guarda la tabla game). Es lo que se fotografía
 * antes de cada acción para poder deshacerla. Campos públicos para serializarlo con Jackson.
 */
public class EstadoVivo {

    public Partido.Estado estado;
    public int entradasProgramadas = 9;
    public int entrada = 1;
    public Mitad mitad = Mitad.TOP;
    public int outs;
    public int bolas;
    public int strikes;
    public Long bateador;
    public Long lanzador;
    /** Índices 1..3 = primera, segunda, tercera (el 0 no se usa). */
    public Long[] bases = new Long[4];
    public int proximoLocal = 1;
    public int proximoVisita = 1;
    public int carrerasLocal;
    public int carrerasVisita;
    public int hitsLocal;
    public int hitsVisita;
    public int erroresLocal;
    public int erroresVisita;

    public boolean bateaLocal() {
        return mitad == Mitad.BOTTOM;
    }

    public int proximo(boolean local) {
        return local ? proximoLocal : proximoVisita;
    }

    public void avanzarOrden(boolean local) {
        if (local) {
            proximoLocal = proximoLocal % 9 + 1;
        } else {
            proximoVisita = proximoVisita % 9 + 1;
        }
    }

    public void sumarCarreras(boolean local, int n) {
        if (local) {
            carrerasLocal += n;
        } else {
            carrerasVisita += n;
        }
    }

    public void sumarHit(boolean local) {
        if (local) {
            hitsLocal++;
        } else {
            hitsVisita++;
        }
    }

    public void sumarErrores(boolean local, int n) {
        if (local) {
            erroresLocal += n;
        } else {
            erroresVisita += n;
        }
    }

    public void limpiarBases() {
        bases = new Long[4];
    }

    public static EstadoVivo de(Partido p) {
        EstadoVivo s = new EstadoVivo();
        s.estado = p.getEstado();
        s.entradasProgramadas = p.getEntradasProgramadas();
        s.entrada = p.getEntradaActual();
        s.mitad = p.getMitad();
        s.outs = p.getOuts();
        s.bolas = p.getBolas();
        s.strikes = p.getStrikes();
        s.bateador = id(p.getBateador());
        s.lanzador = id(p.getLanzador());
        s.bases[1] = id(p.getCorredor1b());
        s.bases[2] = id(p.getCorredor2b());
        s.bases[3] = id(p.getCorredor3b());
        s.proximoLocal = p.getProximoOrdenLocal();
        s.proximoVisita = p.getProximoOrdenVisita();
        s.carrerasLocal = p.getCarrerasLocal();
        s.carrerasVisita = p.getCarrerasVisita();
        s.hitsLocal = p.getHitsLocal();
        s.hitsVisita = p.getHitsVisita();
        s.erroresLocal = p.getErroresLocal();
        s.erroresVisita = p.getErroresVisita();
        return s;
    }

    /** Copia la situación al partido. [jugador] da la referencia JPA de cada id. */
    public void aplicarA(Partido p, Function<Long, Jugador> jugador) {
        p.setEstado(estado);
        p.setEntradaActual(entrada);
        p.setMitad(mitad);
        p.setOuts(outs);
        p.setBolas(bolas);
        p.setStrikes(strikes);
        p.setBateador(ref(bateador, jugador));
        p.setLanzador(ref(lanzador, jugador));
        p.setCorredor1b(ref(bases[1], jugador));
        p.setCorredor2b(ref(bases[2], jugador));
        p.setCorredor3b(ref(bases[3], jugador));
        p.setProximoOrdenLocal(proximoLocal);
        p.setProximoOrdenVisita(proximoVisita);
        p.setCarrerasLocal(carrerasLocal);
        p.setCarrerasVisita(carrerasVisita);
        p.setHitsLocal(hitsLocal);
        p.setHitsVisita(hitsVisita);
        p.setErroresLocal(erroresLocal);
        p.setErroresVisita(erroresVisita);
    }

    private static Long id(Jugador j) {
        return j == null ? null : j.getId();
    }

    private static Jugador ref(Long id, Function<Long, Jugador> jugador) {
        return id == null ? null : jugador.apply(id);
    }
}
