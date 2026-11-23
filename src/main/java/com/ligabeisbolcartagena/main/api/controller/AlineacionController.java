package com.ligabeisbolcartagena.main.api.controller;

import com.ligabeisbolcartagena.main.api.dto.AlineacionDtos.AlineacionDto;
import com.ligabeisbolcartagena.main.api.dto.AlineacionDtos.AlineacionRequest;
import com.ligabeisbolcartagena.main.api.dto.AlineacionDtos.PitcheoDto;
import com.ligabeisbolcartagena.main.service.AlineacionService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partidos/{partidoId}/alineacion")
public class AlineacionController {

    private final AlineacionService alineaciones;

    public AlineacionController(AlineacionService alineaciones) {
        this.alineaciones = alineaciones;
    }

    /** Público cuando el partido ya tiene alineación publicada; admin y anotador siempre la ven. */
    @GetMapping
    public AlineacionDto obtener(@PathVariable Long partidoId, Authentication auth) {
        return alineaciones.obtener(partidoId, privilegiado(auth));
    }

    /** Alineación de pitcheo: lanzador inicial y relevistas de cada equipo. */
    @GetMapping("/pitcheo")
    public PitcheoDto pitcheo(@PathVariable Long partidoId, Authentication auth) {
        return alineaciones.pitcheo(partidoId, privilegiado(auth));
    }

    @PutMapping
    public AlineacionDto guardar(@PathVariable Long partidoId, @Valid @RequestBody AlineacionRequest req) {
        return alineaciones.guardar(partidoId, req);
    }

    private static boolean privilegiado(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_ANOTADOR"));
    }
}
