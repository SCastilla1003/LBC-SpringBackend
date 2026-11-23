package com.ligabeisbolcartagena.main.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Movimiento de un corredor (o del bateador, base 0) dentro de una jugada. */
@Entity
@Table(name = "game_play_movement")
public class MovimientoJugada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "play_id")
    private Jugada jugada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jugador_id")
    private Jugador jugador;

    /** 0 = bateador, 1..3 = base. */
    private int baseDesde;

    /** 1..3 = base, 4 = anotó (home); nulo si es out. */
    private Integer baseHasta;

    private boolean esOut;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Jugada getJugada() { return jugada; }
    public void setJugada(Jugada jugada) { this.jugada = jugada; }
    public Jugador getJugador() { return jugador; }
    public void setJugador(Jugador jugador) { this.jugador = jugador; }
    public int getBaseDesde() { return baseDesde; }
    public void setBaseDesde(int baseDesde) { this.baseDesde = baseDesde; }
    public Integer getBaseHasta() { return baseHasta; }
    public void setBaseHasta(Integer baseHasta) { this.baseHasta = baseHasta; }
    public boolean isEsOut() { return esOut; }
    public void setEsOut(boolean esOut) { this.esOut = esOut; }
}
