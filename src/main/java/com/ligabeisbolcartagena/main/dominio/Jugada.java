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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Jugada cerrada (turno al bate o evento de corredores). Fuente de verdad de las estadísticas. */
@Entity
@Table(name = "game_play")
public class Jugada {

    public enum Resultado {
        SINGLE, DOUBLE, TRIPLE, HOME_RUN,
        WALK, HIT_BY_PITCH, STRIKEOUT,
        GROUND_OUT, FLY_OUT, LINE_OUT, POP_OUT,
        FIELDERS_CHOICE, REACHED_ON_ERROR,
        SACRIFICE_BUNT, SACRIFICE_FLY, DOUBLE_PLAY,
        STOLEN_BASE, CAUGHT_STEALING, WILD_PITCH, PASSED_BALL, BALK
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    private Partido partido;

    private int secuencia;

    @Column(name = "command_id", nullable = false)
    private String commandId;

    @Column(name = "ocurrido_en", nullable = false)
    private LocalDateTime ocurridoEn;

    private int entrada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mitad mitad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bateador_id")
    private Jugador bateador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lanzador_id")
    private Jugador lanzador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Resultado resultado;

    /** Quien cometió el error o hizo la out (para errores y defensa). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jugador_defensa_id")
    private Jugador jugadorDefensa;

    private int carreras;

    @Column(name = "outs_registrados")
    private int outsRegistrados;

    /** Foto del estado del partido antes de la jugada; sirve para deshacerla. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "estado_antes", nullable = false)
    private String estadoAntes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anotador_id")
    private UsuarioCuenta anotador;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Partido getPartido() { return partido; }
    public void setPartido(Partido partido) { this.partido = partido; }
    public int getSecuencia() { return secuencia; }
    public void setSecuencia(int secuencia) { this.secuencia = secuencia; }
    public String getCommandId() { return commandId; }
    public void setCommandId(String commandId) { this.commandId = commandId; }
    public LocalDateTime getOcurridoEn() { return ocurridoEn; }
    public void setOcurridoEn(LocalDateTime ocurridoEn) { this.ocurridoEn = ocurridoEn; }
    public int getEntrada() { return entrada; }
    public void setEntrada(int entrada) { this.entrada = entrada; }
    public Mitad getMitad() { return mitad; }
    public void setMitad(Mitad mitad) { this.mitad = mitad; }
    public Jugador getBateador() { return bateador; }
    public void setBateador(Jugador bateador) { this.bateador = bateador; }
    public Jugador getLanzador() { return lanzador; }
    public void setLanzador(Jugador lanzador) { this.lanzador = lanzador; }
    public Resultado getResultado() { return resultado; }
    public void setResultado(Resultado resultado) { this.resultado = resultado; }
    public Jugador getJugadorDefensa() { return jugadorDefensa; }
    public void setJugadorDefensa(Jugador jugadorDefensa) { this.jugadorDefensa = jugadorDefensa; }
    public int getCarreras() { return carreras; }
    public void setCarreras(int carreras) { this.carreras = carreras; }
    public int getOutsRegistrados() { return outsRegistrados; }
    public void setOutsRegistrados(int outsRegistrados) { this.outsRegistrados = outsRegistrados; }
    public String getEstadoAntes() { return estadoAntes; }
    public void setEstadoAntes(String estadoAntes) { this.estadoAntes = estadoAntes; }
    public UsuarioCuenta getAnotador() { return anotador; }
    public void setAnotador(UsuarioCuenta anotador) { this.anotador = anotador; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
