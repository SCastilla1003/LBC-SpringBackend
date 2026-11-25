package com.ligabeisbolcartagena.main.api.controller;

import com.ligabeisbolcartagena.main.api.dto.JugadorDtos.JugadorDto;
import com.ligabeisbolcartagena.main.api.dto.JugadorDtos.JugadorRequest;
import com.ligabeisbolcartagena.main.api.dto.AuditoriaDtos.ImpactoDto;
import com.ligabeisbolcartagena.main.service.BorradoService;
import com.ligabeisbolcartagena.main.service.JugadorService;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/jugadores")
public class JugadorController {

    private final JugadorService jugadores;
    private final BorradoService borrado;

    public JugadorController(JugadorService jugadores, BorradoService borrado) {
        this.jugadores = jugadores;
        this.borrado = borrado;
    }

    @GetMapping
    public List<JugadorDto> listar() {
        return jugadores.listar();
    }

    @GetMapping("/{id}")
    public JugadorDto obtener(@PathVariable Long id) {
        return jugadores.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JugadorDto crear(@Valid @RequestBody JugadorRequest req) {
        return jugadores.crear(req);
    }

    @PutMapping("/{id}")
    public JugadorDto actualizar(@PathVariable Long id, @Valid @RequestBody JugadorRequest req) {
        return jugadores.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean completo) {
        if (completo) {
            borrado.eliminarJugador(id);
        } else {
            jugadores.eliminar(id);
        }
    }

    /** Lo que se borraría con "eliminar por completo". Solo ADMIN. */
    @GetMapping("/{id}/impacto")
    public ImpactoDto impacto(@PathVariable Long id) {
        return borrado.impactoJugador(id);
    }
}
