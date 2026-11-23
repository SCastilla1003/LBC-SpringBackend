package com.ligabeisbolcartagena.main.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;

/** Tabla de posiciones de un equipo en una temporada. Se recalcula al finalizar cada partido. */
@Entity
@Table(name = "standing")
public class Clasificacion {

    @Embeddable
    public record Clave(Long seasonId, Long teamId) implements Serializable {
    }

    @EmbeddedId
    private Clave clave;

    private int juegos;
    private int ganados;
    private int perdidos;

    private BigDecimal pct = BigDecimal.ZERO;

    @Column(name = "carreras_favor")
    private int carrerasFavor;

    @Column(name = "carreras_contra")
    private int carrerasContra;

    public Clave getClave() { return clave; }
    public void setClave(Clave clave) { this.clave = clave; }
    public int getJuegos() { return juegos; }
    public void setJuegos(int juegos) { this.juegos = juegos; }
    public int getGanados() { return ganados; }
    public void setGanados(int ganados) { this.ganados = ganados; }
    public int getPerdidos() { return perdidos; }
    public void setPerdidos(int perdidos) { this.perdidos = perdidos; }
    public BigDecimal getPct() { return pct; }
    public void setPct(BigDecimal pct) { this.pct = pct; }
    public int getCarrerasFavor() { return carrerasFavor; }
    public void setCarrerasFavor(int carrerasFavor) { this.carrerasFavor = carrerasFavor; }
    public int getCarrerasContra() { return carrerasContra; }
    public void setCarrerasContra(int carrerasContra) { this.carrerasContra = carrerasContra; }
}
