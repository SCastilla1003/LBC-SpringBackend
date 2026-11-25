package com.ligabeisbolcartagena.main.api.controller;

import com.ligabeisbolcartagena.main.api.dto.AuditoriaDtos.PaginaAuditoria;
import com.ligabeisbolcartagena.main.service.AuditoriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Registro de auditoría. Solo ADMIN. */
@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoria;

    public AuditoriaController(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @GetMapping
    public PaginaAuditoria listar(@RequestParam(required = false) String entidad,
                                  @RequestParam(required = false) String texto,
                                  @RequestParam(defaultValue = "0") int pagina,
                                  @RequestParam(defaultValue = "50") int tamano) {
        return auditoria.listar(entidad, texto, pagina, tamano);
    }
}
