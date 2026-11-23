package com.ligabeisbolcartagena.main.api.controller;

import com.ligabeisbolcartagena.main.api.dto.EstadioDtos.EstadioDto;
import com.ligabeisbolcartagena.main.api.dto.EstadioDtos.EstadioRequest;
import com.ligabeisbolcartagena.main.service.EstadioService;
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
@RequestMapping("/api/v1/estadios")
public class EstadioController {

    private final EstadioService estadios;

    public EstadioController(EstadioService estadios) {
        this.estadios = estadios;
    }

    @GetMapping
    public List<EstadioDto> listar() {
        return estadios.listar();
    }

    @GetMapping("/{id}")
    public EstadioDto obtener(@PathVariable Long id) {
        return estadios.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EstadioDto crear(@Valid @RequestBody EstadioRequest req) {
        return estadios.crear(req);
    }

    @PutMapping("/{id}")
    public EstadioDto actualizar(@PathVariable Long id, @Valid @RequestBody EstadioRequest req) {
        return estadios.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        estadios.eliminar(id);
    }
}
