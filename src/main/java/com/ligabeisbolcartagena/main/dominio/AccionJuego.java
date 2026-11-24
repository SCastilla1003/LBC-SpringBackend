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

/** Acción del anotador con la foto del estado anterior (para deshacer y para el jugada a jugada). */
@Entity
@Table(name = "game_action")
public class AccionJuego {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    private Partido partido;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private String descripcion;

    private int entrada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mitad mitad;

    @Column(name = "play_id")
    private Long jugadaId;

    @Column(name = "pitch_id")
    private Long lanzamientoId;

    @Column(name = "substitution_id")
    private Long sustitucionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "estado_antes", nullable = false)
    private String estadoAntes;

    @Column(name = "command_id", nullable = false)
    private String commandId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anotador_id")
    private UsuarioCuenta anotador;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    public Long getId() { return id; }
    public Partido getPartido() { return partido; }
    public void setPartido(Partido partido) { this.partido = partido; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public int getEntrada() { return entrada; }
    public void setEntrada(int entrada) { this.entrada = entrada; }
    public Mitad getMitad() { return mitad; }
    public void setMitad(Mitad mitad) { this.mitad = mitad; }
    public Long getJugadaId() { return jugadaId; }
    public void setJugadaId(Long jugadaId) { this.jugadaId = jugadaId; }
    public Long getLanzamientoId() { return lanzamientoId; }
    public void setLanzamientoId(Long lanzamientoId) { this.lanzamientoId = lanzamientoId; }
    public Long getSustitucionId() { return sustitucionId; }
    public void setSustitucionId(Long sustitucionId) { this.sustitucionId = sustitucionId; }
    public String getEstadoAntes() { return estadoAntes; }
    public void setEstadoAntes(String estadoAntes) { this.estadoAntes = estadoAntes; }
    public String getCommandId() { return commandId; }
    public void setCommandId(String commandId) { this.commandId = commandId; }
    public UsuarioCuenta getAnotador() { return anotador; }
    public void setAnotador(UsuarioCuenta anotador) { this.anotador = anotador; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
