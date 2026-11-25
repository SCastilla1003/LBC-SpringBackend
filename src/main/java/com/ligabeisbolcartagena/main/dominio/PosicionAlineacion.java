package com.ligabeisbolcartagena.main.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Alineación inicial. ordenBateo 1..9 = titular al bate; nulo = banca. */
@Entity
@Table(name = "lineup_spot")
public class PosicionAlineacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    private Partido partido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id")
    private Equipo equipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id")
    private Jugador jugador;

    private Integer ordenBateo;

    private String posicion;

    private boolean titular;

    /** Es el lanzador inicial (puede estar además en el orden de bateo como P o como DH). */
    @Column(name = "lanzador_inicial")
    private boolean lanzadorInicial;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Partido getPartido() { return partido; }
    public void setPartido(Partido partido) { this.partido = partido; }
    public Equipo getEquipo() { return equipo; }
    public void setEquipo(Equipo equipo) { this.equipo = equipo; }
    public Jugador getJugador() { return jugador; }
    public void setJugador(Jugador jugador) { this.jugador = jugador; }
    public Integer getOrdenBateo() { return ordenBateo; }
    public void setOrdenBateo(Integer ordenBateo) { this.ordenBateo = ordenBateo; }
    public String getPosicion() { return posicion; }
    public void setPosicion(String posicion) { this.posicion = posicion; }
    public boolean isTitular() { return titular; }
    public void setTitular(boolean titular) { this.titular = titular; }
    public boolean isLanzadorInicial() { return lanzadorInicial; }
    public void setLanzadorInicial(boolean lanzadorInicial) { this.lanzadorInicial = lanzadorInicial; }
}
