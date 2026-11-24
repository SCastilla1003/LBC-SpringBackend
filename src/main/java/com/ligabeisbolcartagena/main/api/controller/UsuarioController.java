package com.ligabeisbolcartagena.main.api.controller;

import com.ligabeisbolcartagena.main.api.dto.UsuarioDtos.CambioRolRequest;
import com.ligabeisbolcartagena.main.api.dto.UsuarioDtos.UsuarioAdminDto;
import com.ligabeisbolcartagena.main.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Usuarios registrados (solo ADMIN, incluso para leer). */
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    public List<UsuarioAdminDto> listar() {
        return usuarios.listar();
    }

    @PutMapping("/{id}/rol")
    public UsuarioAdminDto cambiarRol(@PathVariable Long id, @Valid @RequestBody CambioRolRequest req,
                                      @AuthenticationPrincipal Jwt jwt) {
        return usuarios.cambiarRol(id, req.rol(), jwt.getSubject());
    }
}
