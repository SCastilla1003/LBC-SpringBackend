package com.ligabeisbolcartagena.main.api.error;

import org.springframework.http.HttpStatus;

/** Códigos de error de la API y su status HTTP. */
public enum CodigoError {
    VALIDACION(HttpStatus.BAD_REQUEST),
    SOLICITUD_INVALIDA(HttpStatus.BAD_REQUEST),
    NO_AUTENTICADO(HttpStatus.UNAUTHORIZED),
    CREDENCIALES_INVALIDAS(HttpStatus.UNAUTHORIZED),
    ACCESO_DENEGADO(HttpStatus.FORBIDDEN),
    NO_ENCONTRADO(HttpStatus.NOT_FOUND),
    METODO_NO_PERMITIDO(HttpStatus.METHOD_NOT_ALLOWED),
    CONFLICTO(HttpStatus.CONFLICT),
    ARCHIVO_DEMASIADO_GRANDE(HttpStatus.PAYLOAD_TOO_LARGE),
    TIPO_NO_SOPORTADO(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    SERVICIO_NO_DISPONIBLE(HttpStatus.SERVICE_UNAVAILABLE),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    CodigoError(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
