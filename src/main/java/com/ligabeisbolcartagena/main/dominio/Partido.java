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
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/** Partido. Guarda el marcador y la situación en vivo; las jugadas están en {@link Jugada}. */
@Entity
@Table(name = "game")
public class Partido {

    public enum Estado { SCHEDULED, LINEUP_PUBLISHED, LIVE, FINISHED, SUSPENDED }

    /** Por qué se detuvo el partido. El administrador decide luego si sigue o se deja así. */
    public enum Motivo { RETRASADO, POSPUESTO, CANCELADO }

    /** Cómo terminó un partido que el local ganó en su último turno. */
    public enum FinalEspecial { WALK_OFF, WALK_OFF_HOME_RUN, WALK_OFF_GRAND_SLAM }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id")
    private Temporada temporada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_team_id")
    private Equipo equipoLocal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "away_team_id")
    private Equipo equipoVisita;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id")
    private Estadio estadio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.SCHEDULED;

    private int entradasProgramadas = 9;

    private int carrerasLocal;
    private int carrerasVisita;
    private int hitsLocal;
    private int hitsVisita;
    private int erroresLocal;
    private int erroresVisita;

    private int entradaActual = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mitad mitad = Mitad.TOP;

    private int outs;
    private int bolas;
    private int strikes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bateador_id")
    private Jugador bateador;

    /** Lanzador actual del equipo que defiende. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lanzador_id")
    private Jugador lanzador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corredor_1b_id")
    private Jugador corredor1b;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corredor_2b_id")
    private Jugador corredor2b;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corredor_3b_id")
    private Jugador corredor3b;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_detencion")
    private Motivo motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_especial")
    private FinalEspecial finalEspecial;

    /** Siguiente turno del orden de bateo (1..9) de cada equipo. */
    private int proximoOrdenLocal = 1;
    private int proximoOrdenVisita = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anotador_id")
    private UsuarioCuenta anotador;

    /** Bloqueo optimista: detecta escrituras simultáneas sobre el mismo partido. */
    @Version
    private Long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Temporada getTemporada() { return temporada; }
    public void setTemporada(Temporada temporada) { this.temporada = temporada; }
    public Equipo getEquipoLocal() { return equipoLocal; }
    public void setEquipoLocal(Equipo equipoLocal) { this.equipoLocal = equipoLocal; }
    public Equipo getEquipoVisita() { return equipoVisita; }
    public void setEquipoVisita(Equipo equipoVisita) { this.equipoVisita = equipoVisita; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public Estadio getEstadio() { return estadio; }
    public void setEstadio(Estadio estadio) { this.estadio = estadio; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public int getEntradasProgramadas() { return entradasProgramadas; }
    public void setEntradasProgramadas(int entradasProgramadas) { this.entradasProgramadas = entradasProgramadas; }
    public int getCarrerasLocal() { return carrerasLocal; }
    public void setCarrerasLocal(int carrerasLocal) { this.carrerasLocal = carrerasLocal; }
    public int getCarrerasVisita() { return carrerasVisita; }
    public void setCarrerasVisita(int carrerasVisita) { this.carrerasVisita = carrerasVisita; }
    public int getHitsLocal() { return hitsLocal; }
    public void setHitsLocal(int hitsLocal) { this.hitsLocal = hitsLocal; }
    public int getHitsVisita() { return hitsVisita; }
    public void setHitsVisita(int hitsVisita) { this.hitsVisita = hitsVisita; }
    public int getErroresLocal() { return erroresLocal; }
    public void setErroresLocal(int erroresLocal) { this.erroresLocal = erroresLocal; }
    public int getErroresVisita() { return erroresVisita; }
    public void setErroresVisita(int erroresVisita) { this.erroresVisita = erroresVisita; }
    public int getEntradaActual() { return entradaActual; }
    public void setEntradaActual(int entradaActual) { this.entradaActual = entradaActual; }
    public Mitad getMitad() { return mitad; }
    public void setMitad(Mitad mitad) { this.mitad = mitad; }
    public int getOuts() { return outs; }
    public void setOuts(int outs) { this.outs = outs; }
    public int getBolas() { return bolas; }
    public void setBolas(int bolas) { this.bolas = bolas; }
    public int getStrikes() { return strikes; }
    public void setStrikes(int strikes) { this.strikes = strikes; }
    public Jugador getBateador() { return bateador; }
    public void setBateador(Jugador bateador) { this.bateador = bateador; }
    public Jugador getLanzador() { return lanzador; }
    public void setLanzador(Jugador lanzador) { this.lanzador = lanzador; }
    public Jugador getCorredor1b() { return corredor1b; }
    public void setCorredor1b(Jugador corredor1b) { this.corredor1b = corredor1b; }
    public Jugador getCorredor2b() { return corredor2b; }
    public void setCorredor2b(Jugador corredor2b) { this.corredor2b = corredor2b; }
    public Jugador getCorredor3b() { return corredor3b; }
    public void setCorredor3b(Jugador corredor3b) { this.corredor3b = corredor3b; }
    public int getProximoOrdenLocal() { return proximoOrdenLocal; }
    public void setProximoOrdenLocal(int proximoOrdenLocal) { this.proximoOrdenLocal = proximoOrdenLocal; }
    public int getProximoOrdenVisita() { return proximoOrdenVisita; }
    public void setProximoOrdenVisita(int proximoOrdenVisita) { this.proximoOrdenVisita = proximoOrdenVisita; }
    public Motivo getMotivo() { return motivo; }
    public void setMotivo(Motivo motivo) { this.motivo = motivo; }
    public FinalEspecial getFinalEspecial() { return finalEspecial; }
    public void setFinalEspecial(FinalEspecial finalEspecial) { this.finalEspecial = finalEspecial; }
    public UsuarioCuenta getAnotador() { return anotador; }
    public void setAnotador(UsuarioCuenta anotador) { this.anotador = anotador; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
