package com.ligabeisbolcartagena.main.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "team")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String abreviatura;

    private String ciudad;

    /** Clave de la paleta de colores de equipos. */
    private String color;

    /** URL del escudo en el bucket de Railway. */
    @Column(name = "escudo_url")
    private String escudoUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getAbreviatura() { return abreviatura; }
    public void setAbreviatura(String abreviatura) { this.abreviatura = abreviatura; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getEscudoUrl() { return escudoUrl; }
    public void setEscudoUrl(String escudoUrl) { this.escudoUrl = escudoUrl; }
}
