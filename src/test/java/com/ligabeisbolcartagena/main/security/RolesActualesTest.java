package com.ligabeisbolcartagena.main.security;

import com.ligabeisbolcartagena.main.dominio.UsuarioCuenta;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuenta.Rol;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuentaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RolesActualesTest {

    static Jwt token(String sub, String rolDelToken) {
        return Jwt.withTokenValue("t").header("alg", "HS256").subject(sub)
                .claim("roles", List.of(rolDelToken)).issuedAt(Instant.now()).build();
    }

    @Test
    void usaElRolDeLaBaseDeDatosYNoElDelToken() {
        UsuarioCuentaRepository repo = mock(UsuarioCuentaRepository.class);
        UsuarioCuenta ana = new UsuarioCuenta();
        ana.setUsername("ana");
        ana.setRol(Rol.ADMIN);
        when(repo.findByUsername("ana")).thenReturn(Optional.of(ana));

        var auth = new RolesActuales(repo).convert(token("ana", "FAN"));

        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
        assertThat(auth.getName()).isEqualTo("ana");
    }

    @Test
    void cuentaDesactivadaOBorradaNoTienePermisos() {
        UsuarioCuentaRepository repo = mock(UsuarioCuentaRepository.class);
        UsuarioCuenta luis = new UsuarioCuenta();
        luis.setUsername("luis");
        luis.setRol(Rol.ADMIN);
        luis.setActiva(false);
        when(repo.findByUsername("luis")).thenReturn(Optional.of(luis));
        when(repo.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThat(new RolesActuales(repo).convert(token("luis", "ADMIN")).getAuthorities()).isEmpty();
        assertThat(new RolesActuales(repo).convert(token("nadie", "ADMIN")).getAuthorities()).isEmpty();
    }
}
