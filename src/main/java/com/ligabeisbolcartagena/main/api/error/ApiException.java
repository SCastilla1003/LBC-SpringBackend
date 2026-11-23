package com.ligabeisbolcartagena.main.api.error;

import java.util.Map;

/** Excepción de negocio que se traduce directamente a un {@link ApiError}. */
public class ApiException extends RuntimeException {

    private final CodigoError codigo;
    private final transient Map<String, Object> detalles;

    public ApiException(CodigoError codigo, String mensaje) {
        this(codigo, mensaje, Map.of());
    }

    public ApiException(CodigoError codigo, String mensaje, Map<String, Object> detalles) {
        super(mensaje);
        this.codigo = codigo;
        this.detalles = detalles == null ? Map.of() : detalles;
    }

    public CodigoError getCodigo() {
        return codigo;
    }

    public Map<String, Object> getDetalles() {
        return detalles;
    }

    // ---- atajos ----

    public static ApiException noEncontrado(String recurso, String id) {
        return new ApiException(CodigoError.NO_ENCONTRADO, recurso + " no encontrado",
                Map.of("recurso", recurso, "id", String.valueOf(id)));
    }

    public static ApiException conflicto(String mensaje) {
        return new ApiException(CodigoError.CONFLICTO, mensaje);
    }

    public static ApiException invalida(String mensaje) {
        return new ApiException(CodigoError.SOLICITUD_INVALIDA, mensaje);
    }

    public static ApiException invalida(String mensaje, Map<String, Object> detalles) {
        return new ApiException(CodigoError.SOLICITUD_INVALIDA, mensaje, detalles);
    }
}
