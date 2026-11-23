package com.ligabeisbolcartagena.main.config;

import com.ligabeisbolcartagena.main.security.RespuestasSeguridadJson;
import com.ligabeisbolcartagena.main.service.UsuarioDetailsService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Seguridad stateless de la API:
 * - GET públicos (temporadas, equipos, jugadores, partidos, estadios, estadísticas, imágenes).
 * - POST /api/v1/auth/** y POST /api/v1/prediccion públicos.
 * - Cualquier otra escritura en /api/v1/** exige rol ADMIN (JWT Bearer).
 * - Sin sesión, sin CSRF (no hay cookies), CORS configurable por variable de entorno.
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                               RespuestasSeguridadJson respuestas,
                                               JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> { }) // usa el bean CorsConfigurationSource
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/registro", "/api/v1/auth/google").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/yo").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/prediccion").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/**").permitAll()
                .requestMatchers("/api/v1/**").hasRole("ADMIN")
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(rs -> rs
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                .authenticationEntryPoint(respuestas.noAutenticado())
                .accessDeniedHandler(respuestas.accesoDenegado())
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(respuestas.noAutenticado())
                .accessDeniedHandler(respuestas.accesoDenegado())
            );
        return http.build();
    }

    // ------------------------------------------------------------------ CORS

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${lbc.cors.origenes-permitidos:}") String origenes) {
        List<String> lista = Arrays.stream(origenes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (lista.isEmpty()) {
            log.warn("CORS_ALLOWED_ORIGINS vacío: ningún navegador podrá llamar a la API (Flutter Web no funcionará).");
        } else {
            log.info("CORS: orígenes permitidos {}", lista);
        }
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(lista); // admite patrones como http://localhost:*
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false); // el JWT viaja en la cabecera, no en cookies
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    // ------------------------------------------------------------------ JWT

    @Bean
    SecretKey claveJwt(@Value("${lbc.jwt.secreto}") String secreto) {
        byte[] bytes = secreto == null ? new byte[0] : secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres (256 bits).");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey claveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey claveJwt, @Value("${lbc.jwt.emisor:lbc-api}") String emisor) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(claveJwt).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(emisor));
        return decoder;
    }

    /** El claim "roles" (["ADMIN"]) se convierte en autoridades ROLE_ADMIN. */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    // ------------------------------------------------------------------ login con usuario y contraseña

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(UsuarioDetailsService usuarioDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
