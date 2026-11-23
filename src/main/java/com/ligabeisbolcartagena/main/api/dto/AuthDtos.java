package com.ligabeisbolcartagena.main.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "El usuario es obligatorio") String username,
            @NotBlank(message = "La contraseña es obligatoria") String password) {
    }

    public record RegistroRequest(
            @NotBlank(message = "El usuario es obligatorio")
            @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
            @Pattern(regexp = "^[\\p{L}0-9._@-]+$", message = "El usuario solo admite letras, números y . _ @ -")
            String username,
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
            String password) {
    }

    /** ID token que la app obtiene con google_sign_in. */
    public record GoogleLoginRequest(@NotBlank(message = "El idToken es obligatorio") String idToken) {
    }

    /** proveedor: "local" o "google". */
    public record UsuarioDto(String username, List<String> roles, String proveedor) {
    }

    public record AuthResponse(String token, String tipo, Instant expiraEn, UsuarioDto usuario) {
    }
}
