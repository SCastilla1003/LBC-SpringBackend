package com.ligabeisbolcartagena.main.security;

import com.ligabeisbolcartagena.main.dominio.UsuarioCuentaRepository;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Los permisos salen del rol que el usuario tiene AHORA en la base de datos, no del
 * claim "roles" del token: así un cambio de rol hecho desde el panel (o desactivar la
 * cuenta) vale desde la siguiente petición, sin esperar a que caduque el JWT.
 */
@Component
public class RolesActuales implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioCuentaRepository usuarios;

    public RolesActuales(UsuarioCuentaRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> autoridades = usuarios.findByUsername(jwt.getSubject())
                .filter(u -> u.isActiva())
                .<List<GrantedAuthority>>map(u -> List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol().name())))
                .orElse(List.of());
        return new JwtAuthenticationToken(jwt, autoridades, jwt.getSubject());
    }
}
