package com.ligabeisbolcartagena.main.api.controller;

import com.ligabeisbolcartagena.main.api.dto.AuthDtos.AuthResponse;
import com.ligabeisbolcartagena.main.api.dto.AuthDtos.GoogleLoginRequest;
import com.ligabeisbolcartagena.main.api.dto.AuthDtos.LoginRequest;
import com.ligabeisbolcartagena.main.api.dto.AuthDtos.RegistroRequest;
import com.ligabeisbolcartagena.main.api.dto.AuthDtos.UsuarioDto;
import com.ligabeisbolcartagena.main.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Usuario y contraseña (BCrypt) → JWT con los roles. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.username(), req.password());
    }

    /** Crea un usuario con rol FAN y devuelve su JWT (queda con la sesión iniciada). */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registro(@Valid @RequestBody RegistroRequest req) {
        return authService.registrar(req.username(), req.password());
    }

    /** ID token de google_sign_in → JWT propio de la API. */
    @PostMapping("/google")
    public AuthResponse google(@Valid @RequestBody GoogleLoginRequest req) {
        return authService.loginGoogle(req.idToken());
    }

    /** Datos del usuario del token (sirve para validar un token guardado al abrir la app). */
    @GetMapping("/yo")
    public UsuarioDto yo(@AuthenticationPrincipal Jwt jwt) {
        return authService.usuario(jwt.getSubject());
    }
}
