package com.ligabeisbolcartagena.main.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class EstadioDtos {

    private EstadioDtos() {
    }

    public record EstadioDto(Long id, String nombre, String ubicacion) {
    }

    public record EstadioRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
            String nombre,
            @Size(max = 200, message = "La ubicación no puede tener más de 200 caracteres")
            String ubicacion) {
    }
}
