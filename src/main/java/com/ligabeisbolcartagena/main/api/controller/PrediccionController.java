package com.ligabeisbolcartagena.main.api.controller;

import com.ligabeisbolcartagena.main.api.dto.PrediccionDtos.OpcionesPrediccionDto;
import com.ligabeisbolcartagena.main.api.dto.PrediccionDtos.PrediccionDto;
import com.ligabeisbolcartagena.main.api.dto.PrediccionDtos.PrediccionRequest;
import com.ligabeisbolcartagena.main.service.PrediccionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Predicción de victoria: el cálculo lo hace el servicio Python (PREDICCION_URL). Público. */
@RestController
@RequestMapping("/api/v1/prediccion")
public class PrediccionController {

    private final PrediccionService prediccion;

    public PrediccionController(PrediccionService prediccion) {
        this.prediccion = prediccion;
    }

    @PostMapping
    public PrediccionDto predecir(@Valid @RequestBody PrediccionRequest req) {
        return prediccion.predecir(req);
    }

    @GetMapping("/opciones")
    public OpcionesPrediccionDto opciones() {
        return prediccion.opciones();
    }
}
