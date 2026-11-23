package com.ligabeisbolcartagena.main.api.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Traduce cualquier excepción de la API a un {@link ApiError} con el status correcto. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> negocio(ApiException e) {
        return responder(e.getCodigo(), e.getMessage(), e.getDetalles());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validacion(MethodArgumentNotValidException e) {
        Map<String, Object> campos = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        e.getBindingResult().getGlobalErrors()
                .forEach(ge -> campos.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage()));
        return responder(CodigoError.VALIDACION, "Hay datos no válidos en la solicitud", campos);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> validacionParametros(HandlerMethodValidationException e) {
        Map<String, Object> campos = new LinkedHashMap<>();
        e.getParameterValidationResults().forEach(r -> campos.put(
                r.getMethodParameter().getParameterName(),
                r.getResolvableErrors().isEmpty() ? "no válido" : r.getResolvableErrors().get(0).getDefaultMessage()));
        return responder(CodigoError.VALIDACION, "Hay parámetros no válidos en la solicitud", campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> jsonInvalido(HttpMessageNotReadableException e) {
        return responder(CodigoError.SOLICITUD_INVALIDA,
                "El cuerpo de la solicitud no es un JSON válido o tiene tipos incorrectos", Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> tipoParametro(MethodArgumentTypeMismatchException e) {
        return responder(CodigoError.SOLICITUD_INVALIDA, "Parámetro con formato no válido: " + e.getName(),
                Map.of("parametro", e.getName()));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    ResponseEntity<ApiError> faltaParametro(Exception e) {
        String nombre = e instanceof MissingServletRequestParameterException p ? p.getParameterName()
                : ((MissingServletRequestPartException) e).getRequestPartName();
        return responder(CodigoError.SOLICITUD_INVALIDA, "Falta el parámetro obligatorio: " + nombre,
                Map.of("parametro", nombre));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> archivoGrande(MaxUploadSizeExceededException e) {
        return responder(CodigoError.ARCHIVO_DEMASIADO_GRANDE, "El archivo supera el tamaño máximo permitido (10 MB)",
                Map.of());
    }

    @ExceptionHandler(MultipartException.class)
    ResponseEntity<ApiError> multipart(MultipartException e) {
        return responder(CodigoError.SOLICITUD_INVALIDA, "La solicitud debe ser multipart/form-data con el archivo",
                Map.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> concurrencia(OptimisticLockingFailureException e) {
        return responder(CodigoError.CONFLICTO,
                "Otro administrador modificó la temporada al mismo tiempo. Recarga e inténtalo de nuevo.", Map.of());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ApiError> duplicado(DuplicateKeyException e) {
        return responder(CodigoError.CONFLICTO, "El recurso ya existe", Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> rutaInexistente(NoResourceFoundException e) {
        return responder(CodigoError.NO_ENCONTRADO, "La ruta no existe", Map.of("ruta", "/" + e.getResourcePath()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> metodo(HttpRequestMethodNotSupportedException e) {
        return responder(CodigoError.METODO_NO_PERMITIDO, "Método " + e.getMethod() + " no permitido en esta ruta",
                Map.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> mediaType(HttpMediaTypeNotSupportedException e) {
        return responder(CodigoError.TIPO_NO_SOPORTADO, "Tipo de contenido no soportado", Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> autenticacion(AuthenticationException e) {
        return responder(CodigoError.NO_AUTENTICADO, "Debes iniciar sesión", Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> acceso(AccessDeniedException e) {
        return responder(CodigoError.ACCESO_DENEGADO, "No tienes permisos para esta acción", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> inesperado(Exception e) {
        log.error("Error no controlado en la API", e);
        return responder(CodigoError.ERROR_INTERNO, "Ocurrió un error interno. Inténtalo más tarde.", Map.of());
    }

    private static ResponseEntity<ApiError> responder(CodigoError codigo, String mensaje, Map<String, Object> detalles) {
        return ResponseEntity.status(codigo.status()).body(ApiError.de(codigo, mensaje, detalles));
    }
}
