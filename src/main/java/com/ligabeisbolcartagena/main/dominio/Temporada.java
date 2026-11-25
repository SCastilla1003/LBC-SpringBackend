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
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "season")
public class Temporada {

    public enum Estado { DRAFT, ACTIVE, FINISHED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private Integer anio;

    @Enumerated(EnumType.STRING)
    private Estado estado = Estado.DRAFT;

    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    /**
     * Muerte súbita: bases donde se ponen corredores automáticos al empezar cada entrada extra,
     * separadas por coma ("2", "1,2"...). Vacío = sin corredores automáticos.
     */
    @Column(name = "bases_muerte_subita", nullable = false)
    private String basesMuerteSubita = "2";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public String getBasesMuerteSubita() { return basesMuerteSubita; }
    public void setBasesMuerteSubita(String basesMuerteSubita) { this.basesMuerteSubita = basesMuerteSubita; }

    /** Bases de la muerte súbita de menor a mayor (1 = primera ... 3 = tercera). */
    public List<Integer> basesMuerteSubita() {
        if (basesMuerteSubita == null || basesMuerteSubita.isBlank()) {
            return List.of();
        }
        return Arrays.stream(basesMuerteSubita.split(",")).map(String::trim).filter(b -> !b.isEmpty())
                .map(Integer::valueOf).sorted().toList();
    }
}
