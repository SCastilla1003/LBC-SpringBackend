package com.ligabeisbolcartagena.main.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.Map;

public final class PrediccionDtos {

    private PrediccionDtos() {
    }

    /**
     * Entradas del modelo (mismo orden y valores que el dataset de entrenamiento).
     * rachaUlt3 = victorias en los últimos 3 partidos (0 a 3).
     */
    public record PrediccionRequest(
            @NotBlank(message = "Indica Local o Visitante")
            @Pattern(regexp = "Local|Visitante", message = "Debe ser Local o Visitante")
            String localVisitante,
            @NotNull(message = "La racha es obligatoria")
            @PositiveOrZero(message = "La racha no puede ser negativa")
            @Max(value = 3, message = "La racha es de los últimos 3 partidos (0 a 3)")
            Double rachaUlt3,
            @NotNull(message = "El promedio de carreras a favor es obligatorio")
            @PositiveOrZero(message = "No puede ser negativo")
            Double promCarrerasFavor,
            @NotNull(message = "El promedio de carreras en contra es obligatorio")
            @PositiveOrZero(message = "No puede ser negativo")
            Double promCarrerasContra,
            @NotBlank(message = "Indica la fuerza del rival")
            @Pattern(regexp = "Baja|Media|Alta", message = "Debe ser Baja, Media o Alta")
            String rivalFuerza) {
    }

    /**
     * clase: "Si" o "No" (¿gana?). confianza: probabilidad de la clase elegida, 0-100.
     * probabilidades: probabilidad (0-100) de cada clase. resumen: explicación en lenguaje claro.
     */
    public record PrediccionDto(
            String clase,
            boolean gana,
            double confianza,
            Map<String, Double> probabilidades,
            PrediccionRequest entradas,
            String resumen) {
    }

    public record RangoDto(double minimo, double maximo, double promedio) {
    }

    /** Valores admitidos y rangos observados en el dataset de entrenamiento (para el formulario). */
    public record OpcionesPrediccionDto(
            List<String> localVisitante,
            List<String> rivalFuerza,
            Map<String, RangoDto> rangos,
            int instanciasEntrenamiento) {
    }
}
