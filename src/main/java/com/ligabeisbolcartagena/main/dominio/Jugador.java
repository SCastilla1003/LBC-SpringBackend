package com.ligabeisbolcartagena.main.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "player")
public class Jugador {

    /** Lado del bateo: D derecho, Z zurdo, A ambidiestro. */
    public enum LadoBateo { D, Z, A }

    /** Lado del lanzamiento: D derecho, Z zurdo. */
    public enum LadoLanzamiento { D, Z }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombres;

    private String apellidos;

    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    private LadoBateo batea;

    @Enumerated(EnumType.STRING)
    private LadoLanzamiento lanza;

    /** URL de la foto en el bucket de Railway. */
    @Column(name = "foto_url")
    private String fotoUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public LadoBateo getBatea() { return batea; }
    public void setBatea(LadoBateo batea) { this.batea = batea; }
    public LadoLanzamiento getLanza() { return lanza; }
    public void setLanza(LadoLanzamiento lanza) { this.lanza = lanza; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
}
