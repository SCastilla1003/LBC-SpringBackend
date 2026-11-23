package com.ligabeisbolcartagena.main.api.controller;

import com.ligabeisbolcartagena.main.api.dto.JugadorDtos.PlantillaActualizacion;
import com.ligabeisbolcartagena.main.api.dto.JugadorDtos.PlantillaDto;
import com.ligabeisbolcartagena.main.api.dto.JugadorDtos.PlantillaRequest;
import com.ligabeisbolcartagena.main.service.PlantillaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/temporadas/{temporadaId}")
public class PlantillaController {

    private final PlantillaService plantillas;

    public PlantillaController(PlantillaService plantillas) {
        this.plantillas = plantillas;
    }

    @GetMapping("/equipos/{equipoId}/plantilla")
    public List<PlantillaDto> plantilla(@PathVariable Long temporadaId, @PathVariable Long equipoId) {
        return plantillas.deEquipo(temporadaId, equipoId);
    }

    @GetMapping("/agentes-libres")
    public List<PlantillaDto> agentesLibres(@PathVariable Long temporadaId) {
        return plantillas.agentesLibres(temporadaId);
    }

    @PostMapping("/plantilla")
    @ResponseStatus(HttpStatus.CREATED)
    public PlantillaDto inscribir(@PathVariable Long temporadaId, @Valid @RequestBody PlantillaRequest req) {
        return plantillas.inscribir(temporadaId, req);
    }

    @PutMapping("/plantilla/{entradaId}")
    public PlantillaDto actualizar(@PathVariable Long temporadaId, @PathVariable Long entradaId,
                                   @Valid @RequestBody PlantillaActualizacion req) {
        return plantillas.actualizar(temporadaId, entradaId, req);
    }

    @DeleteMapping("/plantilla/{entradaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@PathVariable Long temporadaId, @PathVariable Long entradaId) {
        plantillas.quitar(temporadaId, entradaId);
    }
}
