package com.ligabeisbolcartagena.main.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class EquipoDtos {

    private EquipoDtos() {
    }

    public record EquipoDto(Long id, String nombre, String abreviatura, String ciudad, String color,
                            String escudoUrl) {
    }

    /** Inscripción de un equipo en una temporada. */
    public record InscripcionEquipoRequest(@NotNull(message = "El equipo es obligatorio") Long equipoId) {
    }

    public record EquipoRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
            String nombre,
            @NotBlank(message = "La abreviatura es obligatoria")
            @Pattern(regexp = "^[A-Za-z]{2,5}$", message = "La abreviatura debe tener entre 2 y 5 letras")
            String abreviatura,
            @Size(max = 100, message = "La ciudad no puede tener más de 100 caracteres")
            String ciudad,
            @Size(max = 20, message = "El color no puede tener más de 20 caracteres")
            String color,
            @Size(max = 500, message = "La URL del escudo es demasiado larga")
            String escudoUrl) {
    }
}
