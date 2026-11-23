package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.dominio.UsuarioCuenta;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuentaRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/** Carga usuarios locales (usuario + contraseña BCrypt) para el login de la API. */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioCuentaRepository usuarios;

    public UsuarioDetailsService(UsuarioCuentaRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UsuarioCuenta cuenta = usuarios.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // Las cuentas creadas con Google no tienen contraseña: no pueden entrar por este medio
        if (cuenta.getPasswordHash() == null || cuenta.getPasswordHash().isBlank()) {
            throw new UsernameNotFoundException("La cuenta no tiene contraseña local");
        }

        return new User(
                cuenta.getUsername(),
                cuenta.getPasswordHash(),
                cuenta.isActiva(),
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + cuenta.getRol().name()))
        );
    }
}
