package com.ligabeisbolcartagena.main.api.controller;

import com.ligabeisbolcartagena.main.api.dto.JuegoDtos.CambioRequest;
import com.ligabeisbolcartagena.main.api.dto.JuegoDtos.ComandoRequest;
import com.ligabeisbolcartagena.main.api.dto.JuegoDtos.EstadoJuegoDto;
import com.ligabeisbolcartagena.main.api.dto.JuegoDtos.JugadaRequest;
import com.ligabeisbolcartagena.main.api.dto.JuegoDtos.LanzamientoRequest;
import com.ligabeisbolcartagena.main.service.JuegoService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Anotador en vivo. Leer el estado es público; escribir exige ADMIN o ANOTADOR.
 * Cada escritura responde el estado completo ya actualizado.
 */
@RestController
@RequestMapping("/api/v1/partidos/{id}/juego")
public class JuegoController {

    private final JuegoService juego;

    public JuegoController(JuegoService juego) {
        this.juego = juego;
    }

    @GetMapping
    public EstadoJuegoDto estado(@PathVariable Long id) {
        return juego.estado(id);
    }

    @PostMapping("/iniciar")
    public EstadoJuegoDto iniciar(@PathVariable Long id, @Valid @RequestBody ComandoRequest req,
                                  @AuthenticationPrincipal Jwt jwt) {
        return juego.iniciar(id, req.commandId(), jwt.getSubject());
    }

    @PostMapping("/lanzamiento")
    public EstadoJuegoDto lanzamiento(@PathVariable Long id, @Valid @RequestBody LanzamientoRequest req,
                                      @AuthenticationPrincipal Jwt jwt) {
        return juego.lanzamiento(id, req, jwt.getSubject());
    }

    @PostMapping("/jugada")
    public EstadoJuegoDto jugada(@PathVariable Long id, @Valid @RequestBody JugadaRequest req,
                                 @AuthenticationPrincipal Jwt jwt) {
        return juego.jugada(id, req, jwt.getSubject());
    }

    @PostMapping("/cambio")
    public EstadoJuegoDto cambio(@PathVariable Long id, @Valid @RequestBody CambioRequest req,
                                 @AuthenticationPrincipal Jwt jwt) {
        return juego.cambio(id, req, jwt.getSubject());
    }

    @PostMapping("/deshacer")
    public EstadoJuegoDto deshacer(@PathVariable Long id) {
        return juego.deshacer(id);
    }

    @PostMapping("/finalizar")
    public EstadoJuegoDto finalizar(@PathVariable Long id, @Valid @RequestBody ComandoRequest req,
                                    @AuthenticationPrincipal Jwt jwt) {
        return juego.finalizar(id, req.commandId(), jwt.getSubject());
    }

    @PostMapping("/suspender")
    public EstadoJuegoDto suspender(@PathVariable Long id, @Valid @RequestBody ComandoRequest req,
                                    @AuthenticationPrincipal Jwt jwt) {
        return juego.suspender(id, req.commandId(), jwt.getSubject());
    }

    @PostMapping("/reanudar")
    public EstadoJuegoDto reanudar(@PathVariable Long id, @Valid @RequestBody ComandoRequest req,
                                   @AuthenticationPrincipal Jwt jwt) {
        return juego.reanudar(id, req.commandId(), jwt.getSubject());
    }
}
