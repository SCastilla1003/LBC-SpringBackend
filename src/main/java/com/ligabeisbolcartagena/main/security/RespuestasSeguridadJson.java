package com.ligabeisbolcartagena.main.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ligabeisbolcartagena.main.api.error.ApiError;
import com.ligabeisbolcartagena.main.api.error.CodigoError;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Respuestas 401/403 de Spring Security con el mismo formato {@link ApiError} que el resto de la API. */
@Component
public class RespuestasSeguridadJson {

    private final ObjectMapper objectMapper;

    public RespuestasSeguridadJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthenticationEntryPoint noAutenticado() {
        return (request, response, ex) -> {
            boolean conToken = request.getHeader("Authorization") != null;
            escribir(response, CodigoError.NO_AUTENTICADO, conToken
                    ? "La sesión no es válida o expiró. Inicia sesión de nuevo."
                    : "Debes iniciar sesión");
        };
    }

    public AccessDeniedHandler accesoDenegado() {
        return (request, response, ex) ->
                escribir(response, CodigoError.ACCESO_DENEGADO, "No tienes permisos para esta acción");
    }

    private void escribir(HttpServletResponse response, CodigoError codigo, String mensaje) throws IOException {
        response.setStatus(codigo.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (codigo == CodigoError.NO_AUTENTICADO) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        objectMapper.writeValue(response.getOutputStream(), ApiError.de(codigo, mensaje));
    }
}
