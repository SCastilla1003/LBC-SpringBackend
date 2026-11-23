package com.ligabeisbolcartagena.main.api.controller;

import com.ligabeisbolcartagena.main.api.dto.TemporadaDtos.TemporadaDto;
import com.ligabeisbolcartagena.main.api.dto.TemporadaDtos.TemporadaRequest;
import com.ligabeisbolcartagena.main.service.TemporadaService;
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
@RequestMapping("/api/v1")
public class TemporadaController {

    private final TemporadaService temporadas;

    public TemporadaController(TemporadaService temporadas) {
        this.temporadas = temporadas;
    }

    @GetMapping("/temporadas")
    public List<TemporadaDto> listar() {
        return temporadas.listar();
    }

    @GetMapping("/temporadas/actual")
    public TemporadaDto actual() {
        return temporadas.actual();
    }

    @GetMapping("/temporadas/{id}")
    public TemporadaDto obtener(@PathVariable Long id) {
        return temporadas.obtener(id);
    }

    @PostMapping("/temporadas")
    @ResponseStatus(HttpStatus.CREATED)
    public TemporadaDto crear(@Valid @RequestBody TemporadaRequest req) {
        return temporadas.crear(req);
    }

    @PutMapping("/temporadas/{id}")
    public TemporadaDto actualizar(@PathVariable Long id, @Valid @RequestBody TemporadaRequest req) {
        return temporadas.actualizar(id, req);
    }

    @DeleteMapping("/temporadas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        temporadas.eliminar(id);
    }
}
