package com.ligabeisbolcartagena.main.api.controller;

import com.ligabeisbolcartagena.main.api.dto.EquipoDtos.EquipoDto;
import com.ligabeisbolcartagena.main.api.dto.JugadorDtos.JugadorDto;
import com.ligabeisbolcartagena.main.service.AlmacenObjetos;
import com.ligabeisbolcartagena.main.service.FotoService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

/**
 * Fotos de jugadores y escudos de equipos (bucket de Railway).
 * Subir y borrar: ADMIN. Ver: público (el GET de /api/v1/** es público).
 */
@RestController
@RequestMapping("/api/v1")
public class FotoController {

    private final FotoService fotos;

    public FotoController(FotoService fotos) {
        this.fotos = fotos;
    }

    @PostMapping(value = "/jugadores/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public JugadorDto subirFoto(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo)
            throws IOException {
        return fotos.subirFotoJugador(id, archivo.getBytes());
    }

    @DeleteMapping("/jugadores/{id}/foto")
    public JugadorDto borrarFoto(@PathVariable Long id) {
        return fotos.borrarFotoJugador(id);
    }

    @GetMapping("/jugadores/{id}/foto")
    public ResponseEntity<byte[]> verFoto(@PathVariable Long id) {
        return imagen(fotos.fotoJugador(id));
    }

    @PostMapping(value = "/equipos/{id}/escudo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EquipoDto subirEscudo(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo)
            throws IOException {
        return fotos.subirEscudo(id, archivo.getBytes());
    }

    @DeleteMapping("/equipos/{id}/escudo")
    public EquipoDto borrarEscudo(@PathVariable Long id) {
        return fotos.borrarEscudo(id);
    }

    @GetMapping("/equipos/{id}/escudo")
    public ResponseEntity<byte[]> verEscudo(@PathVariable Long id) {
        return imagen(fotos.escudo(id));
    }

    /** La URL guardada lleva ?v=..., así que la imagen se puede guardar en caché un día. */
    private static ResponseEntity<byte[]> imagen(AlmacenObjetos.Objeto o) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(o.tipo() == null ? "image/jpeg" : o.tipo()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(o.datos());
    }
}
