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

/** Cambio durante el partido: pinch hitter, pinch runner, cambio defensivo o de lanzador. */
@Entity
@Table(name = "game_substitution")
public class Sustitucion {

    public enum Tipo { PINCH_HITTER, PINCH_RUNNER, DEFENSIVE, PITCHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    private Partido partido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id")
    private Equipo equipo;

    private int entrada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mitad mitad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jugador_sale_id")
    private Jugador jugadorSale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jugador_entra_id")
    private Jugador jugadorEntra;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    private Integer ordenBateo;

    private String posicion;

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
    public Equipo getEquipo() { return equipo; }
    public void setEquipo(Equipo equipo) { this.equipo = equipo; }
    public int getEntrada() { return entrada; }
    public void setEntrada(int entrada) { this.entrada = entrada; }
    public Mitad getMitad() { return mitad; }
    public void setMitad(Mitad mitad) { this.mitad = mitad; }
    public Jugador getJugadorSale() { return jugadorSale; }
    public void setJugadorSale(Jugador jugadorSale) { this.jugadorSale = jugadorSale; }
    public Jugador getJugadorEntra() { return jugadorEntra; }
    public void setJugadorEntra(Jugador jugadorEntra) { this.jugadorEntra = jugadorEntra; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public Integer getOrdenBateo() { return ordenBateo; }
    public void setOrdenBateo(Integer ordenBateo) { this.ordenBateo = ordenBateo; }
    public String getPosicion() { return posicion; }
    public void setPosicion(String posicion) { this.posicion = posicion; }
    public String getCommandId() { return commandId; }
    public void setCommandId(String commandId) { this.commandId = commandId; }
    public LocalDateTime getOcurridoEn() { return ocurridoEn; }
    public void setOcurridoEn(LocalDateTime ocurridoEn) { this.ocurridoEn = ocurridoEn; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
