package com.ligabeisbolcartagena.main.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Estadísticas acumuladas de un jugador en una temporada. Se recalculan desde las jugadas. */
@Entity
@Table(name = "player_stat")
public class EstadisticaJugador {

    @Embeddable
    public record Clave(Long seasonId, Long playerId) implements Serializable {
    }

    @EmbeddedId
    private Clave clave;

    // Bateo
    private int juegos;
    private int pa;
    private int ab;
    private int h;
    private int dobles;
    private int triples;
    private int hr;
    private int rbi;
    private int carreras;
    private int bb;
    private int hbp;
    private int so;
    private int robos;

    // Lanzador
    private int juegosLanzados;
    private int juegosIniciados;
    private int outsLanzados;
    private int hPermitidos;
    private int carrerasPermitidas;
    private int carrerasLimpias;
    private int bbPermitidos;
    private int soLanzados;
    private int lanzamientos;
    private int victorias;
    private int derrotas;

    // Defensa
    private int errores;

    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;

    public Clave getClave() { return clave; }
    public void setClave(Clave clave) { this.clave = clave; }
    public int getJuegos() { return juegos; }
    public void setJuegos(int juegos) { this.juegos = juegos; }
    public int getPa() { return pa; }
    public void setPa(int pa) { this.pa = pa; }
    public int getAb() { return ab; }
    public void setAb(int ab) { this.ab = ab; }
    public int getH() { return h; }
    public void setH(int h) { this.h = h; }
    public int getDobles() { return dobles; }
    public void setDobles(int dobles) { this.dobles = dobles; }
    public int getTriples() { return triples; }
    public void setTriples(int triples) { this.triples = triples; }
    public int getHr() { return hr; }
    public void setHr(int hr) { this.hr = hr; }
    public int getRbi() { return rbi; }
    public void setRbi(int rbi) { this.rbi = rbi; }
    public int getCarreras() { return carreras; }
    public void setCarreras(int carreras) { this.carreras = carreras; }
    public int getBb() { return bb; }
    public void setBb(int bb) { this.bb = bb; }
    public int getHbp() { return hbp; }
    public void setHbp(int hbp) { this.hbp = hbp; }
    public int getSo() { return so; }
    public void setSo(int so) { this.so = so; }
    public int getRobos() { return robos; }
    public void setRobos(int robos) { this.robos = robos; }
    public int getJuegosLanzados() { return juegosLanzados; }
    public void setJuegosLanzados(int juegosLanzados) { this.juegosLanzados = juegosLanzados; }
    public int getJuegosIniciados() { return juegosIniciados; }
    public void setJuegosIniciados(int juegosIniciados) { this.juegosIniciados = juegosIniciados; }
    public int getOutsLanzados() { return outsLanzados; }
    public void setOutsLanzados(int outsLanzados) { this.outsLanzados = outsLanzados; }
    public int getHPermitidos() { return hPermitidos; }
    public void setHPermitidos(int hPermitidos) { this.hPermitidos = hPermitidos; }
    public int getCarrerasPermitidas() { return carrerasPermitidas; }
    public void setCarrerasPermitidas(int carrerasPermitidas) { this.carrerasPermitidas = carrerasPermitidas; }
    public int getCarrerasLimpias() { return carrerasLimpias; }
    public void setCarrerasLimpias(int carrerasLimpias) { this.carrerasLimpias = carrerasLimpias; }
    public int getBbPermitidos() { return bbPermitidos; }
    public void setBbPermitidos(int bbPermitidos) { this.bbPermitidos = bbPermitidos; }
    public int getSoLanzados() { return soLanzados; }
    public void setSoLanzados(int soLanzados) { this.soLanzados = soLanzados; }
    public int getLanzamientos() { return lanzamientos; }
    public void setLanzamientos(int lanzamientos) { this.lanzamientos = lanzamientos; }
    public int getVictorias() { return victorias; }
    public void setVictorias(int victorias) { this.victorias = victorias; }
    public int getDerrotas() { return derrotas; }
    public void setDerrotas(int derrotas) { this.derrotas = derrotas; }
    public int getErrores() { return errores; }
    public void setErrores(int errores) { this.errores = errores; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
