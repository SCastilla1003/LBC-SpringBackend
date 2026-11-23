package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.error.ApiException;

/** Errores comunes de los servicios. */
final class Errores {

    private Errores() {
    }

    /** Borrar algo que otros registros usan (llave foránea). */
    static ApiException relacionado() {
        return ApiException.conflicto("No se puede borrar porque tiene registros relacionados");
    }
}
