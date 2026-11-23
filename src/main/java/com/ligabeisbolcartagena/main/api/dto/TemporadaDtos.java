package com.ligabeisbolcartagena.main.api.dto;

import com.ligabeisbolcartagena.main.dominio.Temporada;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class TemporadaDtos {

    private TemporadaDtos() {
    }

    public record TemporadaDto(Long id, String nombre, Integer anio, Temporada.Estado estado,
                               LocalDate fechaInicio, LocalDate fechaFin) {
    }

    public record TemporadaRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
            String nombre,
            @NotNull(message = "El año es obligatorio")
            @Min(value = 1900, message = "El año debe ser 1900 o posterior")
            @Max(value = 2100, message = "El año debe ser 2100 o anterior")
            Integer anio,
            Temporada.Estado estado,
            LocalDate fechaInicio,
            LocalDate fechaFin) {
    }
}
