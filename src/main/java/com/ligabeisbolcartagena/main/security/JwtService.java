package com.ligabeisbolcartagena.main.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Emite los JWT propios de la API (HS256). */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final Duration expiracion;
    private final String emisor;

    public JwtService(JwtEncoder encoder,
                      @Value("${lbc.jwt.expiracion:PT24H}") Duration expiracion,
                      @Value("${lbc.jwt.emisor:lbc-api}") String emisor) {
        this.encoder = encoder;
        this.expiracion = expiracion;
        this.emisor = emisor;
    }

    public record TokenEmitido(String valor, Instant expiraEn) {
    }

    public TokenEmitido emitir(String username, List<String> roles, String proveedor) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(expiracion);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emisor)
                .subject(username)
                .issuedAt(ahora)
                .expiresAt(expira)
                .claim("roles", roles == null ? List.of() : roles)
                .claim("proveedor", proveedor)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenEmitido(token, expira);
    }
}
