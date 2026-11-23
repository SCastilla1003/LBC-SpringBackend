package com.ligabeisbolcartagena.main.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Cada lanzamiento: bola, strike, foul o en juego. Sirve para contar lanzamientos. */
@Entity
@Table(name = "game_pitch")
public class Lanzamiento {

    public enum Tipo { BALL, CALLED_STRIKE, SWINGING_STRIKE, FOUL, IN_PLAY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    private Partido partido;

    /** Se asigna cuando la jugada se cierra. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "play_id")
    private Jugada jugada;

    private int entrada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mitad mitad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lanzador_id")
    private Jugador lanzador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bateador_id")
    private Jugador bateador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    @Column(name = "command_id", nullable = false)
    private String commandId;

    @Column(name = "ocurrido_en", nullable = false)
    private LocalDateTime ocurridoEn;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Partido getPartido() { return partido; }
    public void setPartido(Partido partido) { this.partido = partido; }
    public Jugada getJugada() { return jugada; }
    public void setJugada(Jugada jugada) { this.jugada = jugada; }
    public int getEntrada() { return entrada; }
    public void setEntrada(int entrada) { this.entrada = entrada; }
    public Mitad getMitad() { return mitad; }
    public void setMitad(Mitad mitad) { this.mitad = mitad; }
    public Jugador getLanzador() { return lanzador; }
    public void setLanzador(Jugador lanzador) { this.lanzador = lanzador; }
    public Jugador getBateador() { return bateador; }
    public void setBateador(Jugador bateador) { this.bateador = bateador; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public String getCommandId() { return commandId; }
    public void setCommandId(String commandId) { this.commandId = commandId; }
    public LocalDateTime getOcurridoEn() { return ocurridoEn; }
    public void setOcurridoEn(LocalDateTime ocurridoEn) { this.ocurridoEn = ocurridoEn; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
