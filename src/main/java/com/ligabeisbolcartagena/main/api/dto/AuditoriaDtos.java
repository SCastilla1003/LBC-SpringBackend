package com.ligabeisbolcartagena.main.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class AuditoriaDtos {

    private AuditoriaDtos() {
    }

    /** antes y despues: los datos en JSON (texto), o null. */
    public record RegistroDto(Long id, LocalDateTime fecha, String usuario, String accion, String entidad,
                              Long entidadId, String descripcion, String antes, String despues) {
    }

    public record PaginaAuditoria(List<RegistroDto> registros, int pagina, boolean hayMas) {
    }

    /** Lo que se borra junto con un jugador o un partido al eliminarlo por completo. */
    public record ImpactoDto(String nombre, long inscripciones, long alineaciones, long partidos, long jugadas,
                             long lanzamientos, long cambios, boolean enJuego) {
    }
}
