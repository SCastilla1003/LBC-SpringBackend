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

/** Equipo, jugador o partido marcado como favorito por una cuenta. Sin FK: apunta a tablas distintas. */
@Entity
@Table(name = "favorite")
public class Favorito {

    public enum TipoObjetivo { TEAM, PLAYER, GAME }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private UsuarioCuenta cuenta;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TipoObjetivo tipoObjetivo;

    @Column(name = "target_id", nullable = false)
    private Long objetivoId;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UsuarioCuenta getCuenta() { return cuenta; }
    public void setCuenta(UsuarioCuenta cuenta) { this.cuenta = cuenta; }
    public TipoObjetivo getTipoObjetivo() { return tipoObjetivo; }
    public void setTipoObjetivo(TipoObjetivo tipoObjetivo) { this.tipoObjetivo = tipoObjetivo; }
    public Long getObjetivoId() { return objetivoId; }
    public void setObjetivoId(Long objetivoId) { this.objetivoId = objetivoId; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
