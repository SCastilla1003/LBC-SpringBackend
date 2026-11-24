package com.ligabeisbolcartagena.main.api.controller;

import com.ligabeisbolcartagena.main.api.dto.ComunDtos.EquipoResumen;
import com.ligabeisbolcartagena.main.api.dto.EquipoDtos.EquipoDto;
import com.ligabeisbolcartagena.main.api.dto.EquipoDtos.EquipoRequest;
import com.ligabeisbolcartagena.main.api.dto.EquipoDtos.InscripcionEquipoRequest;
import com.ligabeisbolcartagena.main.service.EquipoService;
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
public class EquipoController {

    private final EquipoService equipos;

    public EquipoController(EquipoService equipos) {
        this.equipos = equipos;
    }

    @GetMapping("/equipos")
    public List<EquipoDto> listar() {
        return equipos.listar();
    }

    @GetMapping("/equipos/{id}")
    public EquipoDto obtener(@PathVariable Long id) {
        return equipos.obtener(id);
    }

    @PostMapping("/equipos")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoDto crear(@Valid @RequestBody EquipoRequest req) {
        return equipos.crear(req);
    }

    @PutMapping("/equipos/{id}")
    public EquipoDto actualizar(@PathVariable Long id, @Valid @RequestBody EquipoRequest req) {
        return equipos.actualizar(id, req);
    }

    @DeleteMapping("/equipos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        equipos.eliminar(id);
    }

    @GetMapping("/temporadas/{id}/equipos")
    public List<EquipoResumen> deTemporada(@PathVariable Long id) {
        return equipos.deTemporada(id);
    }

    @PostMapping("/temporadas/{id}/equipos")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoResumen inscribir(@PathVariable Long id, @Valid @RequestBody InscripcionEquipoRequest req) {
        return equipos.inscribir(id, req.equipoId());
    }

    @DeleteMapping("/temporadas/{id}/equipos/{equipoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desinscribir(@PathVariable Long id, @PathVariable Long equipoId) {
        equipos.desinscribir(id, equipoId);
    }
}
