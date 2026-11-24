package com.ligabeisbolcartagena.main.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

public final class UsuarioDtos {

    private UsuarioDtos() {
    }

    /** Usuario registrado, visto desde el panel de administración. proveedor: "local" o "google". */
    public record UsuarioAdminDto(Long id, String username, String email, String rol, String proveedor,
                                  boolean activa, LocalDateTime creadoEn) {
    }

    public record CambioRolRequest(
            @NotBlank(message = "El rol es obligatorio")
            @Pattern(regexp = "ADMIN|ANOTADOR|FAN", message = "El rol debe ser ADMIN, ANOTADOR o FAN")
            String rol) {
    }
}
