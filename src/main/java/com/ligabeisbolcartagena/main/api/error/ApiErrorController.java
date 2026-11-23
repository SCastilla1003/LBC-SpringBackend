package com.ligabeisbolcartagena.main.api.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Último recurso para errores que no pasan por los controladores (p. ej. fallos en filtros):
 * responde con el mismo formato {@link ApiError} en lugar de la página por defecto.
 */
@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiError> error(HttpServletRequest request) {
        Object atributo = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = atributo instanceof Integer i ? i : 500;
        CodigoError codigo = switch (status) {
            case 400 -> CodigoError.SOLICITUD_INVALIDA;
            case 401 -> CodigoError.NO_AUTENTICADO;
            case 403 -> CodigoError.ACCESO_DENEGADO;
            case 404 -> CodigoError.NO_ENCONTRADO;
            case 405 -> CodigoError.METODO_NO_PERMITIDO;
            case 413 -> CodigoError.ARCHIVO_DEMASIADO_GRANDE;
            case 415 -> CodigoError.TIPO_NO_SOPORTADO;
            default -> CodigoError.ERROR_INTERNO;
        };
        String mensaje = codigo == CodigoError.ERROR_INTERNO ? "Ocurrió un error interno. Inténtalo más tarde."
                : "La solicitud no se pudo completar";
        return ResponseEntity.status(status).body(ApiError.de(codigo, mensaje));
    }
}
