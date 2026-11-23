package com.ligabeisbolcartagena.main.api.error;

import java.util.Map;

/**
 * Cuerpo uniforme de todos los errores de la API.
 * codigo: identificador estable para la app (p. ej. NO_ENCONTRADO).
 * mensaje: texto en español apto para mostrar al usuario.
 * detalles: información adicional (p. ej. campo -> mensaje en errores de validación); nunca null.
 */
public record ApiError(String codigo, String mensaje, Map<String, Object> detalles) {

    public static ApiError de(CodigoError codigo, String mensaje) {
        return new ApiError(codigo.name(), mensaje, Map.of());
    }

    public static ApiError de(CodigoError codigo, String mensaje, Map<String, Object> detalles) {
        return new ApiError(codigo.name(), mensaje, detalles == null ? Map.of() : detalles);
    }
}
