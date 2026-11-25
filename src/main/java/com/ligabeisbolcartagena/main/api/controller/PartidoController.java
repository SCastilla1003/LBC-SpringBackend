package com.ligabeisbolcartagena.main.api.controller;

import com.ligabeisbolcartagena.main.api.dto.AuditoriaDtos.ImpactoDto;
import com.ligabeisbolcartagena.main.api.dto.PartidoDtos.PartidoActualizacion;
import com.ligabeisbolcartagena.main.api.dto.PartidoDtos.PartidoDetalleDto;
import com.ligabeisbolcartagena.main.api.dto.PartidoDtos.PartidoRequest;
import com.ligabeisbolcartagena.main.api.dto.PartidoDtos.PartidoResumenDto;
import com.ligabeisbolcartagena.main.api.dto.PartidoDtos.PosicionDto;
import com.ligabeisbolcartagena.main.dominio.Partido;
import com.ligabeisbolcartagena.main.service.BorradoService;
import com.ligabeisbolcartagena.main.service.PartidoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PartidoController {

    private final PartidoService partidos;
    private final BorradoService borrado;

    public PartidoController(PartidoService partidos, BorradoService borrado) {
        this.partidos = partidos;
        this.borrado = borrado;
    }

    @GetMapping("/temporadas/{temporadaId}/partidos")
    public List<PartidoResumenDto> listar(
            @PathVariable Long temporadaId,
            @RequestParam(required = false) Partido.Estado estado,
            @RequestParam(required = false) Long equipoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return partidos.listar(temporadaId, estado, equipoId, desde, hasta);
    }

    @PostMapping("/temporadas/{temporadaId}/partidos")
    @ResponseStatus(HttpStatus.CREATED)
    public PartidoDetalleDto crear(@PathVariable Long temporadaId, @Valid @RequestBody PartidoRequest req) {
        return partidos.crear(temporadaId, req);
    }

    @GetMapping("/temporadas/{temporadaId}/posiciones")
    public List<PosicionDto> posiciones(@PathVariable Long temporadaId) {
        return partidos.posiciones(temporadaId);
    }

    @GetMapping("/partidos/{id}")
    public PartidoDetalleDto obtener(@PathVariable Long id) {
        return partidos.obtener(id);
    }

    @PutMapping("/partidos/{id}")
    public PartidoDetalleDto actualizar(@PathVariable Long id, @Valid @RequestBody PartidoActualizacion req) {
        return partidos.actualizar(id, req);
    }

    @DeleteMapping("/partidos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean completo) {
        if (completo) {
            borrado.eliminarPartido(id);
        } else {
            partidos.eliminar(id);
        }
    }

    /** Lo que se borraría con "eliminar por completo". Solo ADMIN. */
    @GetMapping("/partidos/{id}/impacto")
    public ImpactoDto impacto(@PathVariable Long id) {
        return borrado.impactoPartido(id);
    }
}
