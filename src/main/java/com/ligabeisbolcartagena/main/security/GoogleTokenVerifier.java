package com.ligabeisbolcartagena.main.security;

import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.api.error.CodigoError;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Verifica ID tokens de Google: firma (claves públicas JWKS de Google), emisor,
 * audiencia (uno de nuestros client IDs), expiración y correo verificado.
 * No necesita el client secret.
 */
@Component
public class GoogleTokenVerifier {

    private static final String JWKS_GOOGLE = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> EMISORES = Set.of("accounts.google.com", "https://accounts.google.com");

    private final List<String> clientIds;
    private volatile JwtDecoder decoder;

    public GoogleTokenVerifier(@Value("${lbc.google.client-ids:}") String clientIds) {
        this.clientIds = Arrays.stream(clientIds.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** Datos de la cuenta de Google ya verificados. */
    public record CuentaGoogle(String sub, String email, String nombre) {
    }

    public CuentaGoogle verificar(String idToken) {
        if (clientIds.isEmpty()) {
            throw new ApiException(CodigoError.SERVICIO_NO_DISPONIBLE,
                    "El inicio de sesión con Google no está configurado en el servidor");
        }
        Jwt jwt;
        try {
            jwt = decoder().decode(idToken);
        } catch (JwtException e) {
            throw new ApiException(CodigoError.CREDENCIALES_INVALIDAS, "El token de Google no es válido o expiró");
        }
        Object verificado = jwt.getClaims().get("email_verified");
        String email = jwt.getClaimAsString("email");
        if (email == null || !(Boolean.TRUE.equals(verificado) || "true".equals(String.valueOf(verificado)))) {
            throw new ApiException(CodigoError.CREDENCIALES_INVALIDAS, "La cuenta de Google no tiene un correo verificado");
        }
        return new CuentaGoogle(jwt.getSubject(), email, jwt.getClaimAsString("name"));
    }

    private JwtDecoder decoder() {
        if (decoder == null) {
            synchronized (this) {
                if (decoder == null) {
                    NimbusJwtDecoder d = NimbusJwtDecoder.withJwkSetUri(JWKS_GOOGLE).build();
                    OAuth2TokenValidator<Jwt> emisor = jwt -> EMISORES.contains(String.valueOf(jwt.getClaims().get("iss")))
                            ? OAuth2TokenValidatorResult.success()
                            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Emisor no válido", null));
                    OAuth2TokenValidator<Jwt> audiencia = jwt -> jwt.getAudience() != null
                            && jwt.getAudience().stream().anyMatch(clientIds::contains)
                            ? OAuth2TokenValidatorResult.success()
                            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Audiencia no válida", null));
                    d.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), emisor, audiencia));
                    decoder = d;
                }
            }
        }
        return decoder;
    }
}
