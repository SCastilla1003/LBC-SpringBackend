package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.AuthDtos.AuthResponse;
import com.ligabeisbolcartagena.main.api.dto.AuthDtos.UsuarioDto;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.api.error.CodigoError;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuenta;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuenta.Proveedor;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuenta.Rol;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuentaRepository;
import com.ligabeisbolcartagena.main.security.GoogleTokenVerifier;
import com.ligabeisbolcartagena.main.security.GoogleTokenVerifier.CuentaGoogle;
import com.ligabeisbolcartagena.main.security.JwtService;
import com.ligabeisbolcartagena.main.security.JwtService.TokenEmitido;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioCuentaRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthService(AuthenticationManager authenticationManager, UsuarioCuentaRepository usuarios,
                       PasswordEncoder passwordEncoder, JwtService jwtService, GoogleTokenVerifier googleTokenVerifier) {
        this.authenticationManager = authenticationManager;
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    public AuthResponse login(String username, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username.trim(), password));
        } catch (AuthenticationException e) {
            throw new ApiException(CodigoError.CREDENCIALES_INVALIDAS, "Usuario o contraseña incorrectos");
        }
        UsuarioCuenta cuenta = usuarios.findByUsername(username.trim())
                .orElseThrow(() -> new ApiException(CodigoError.CREDENCIALES_INVALIDAS, "Usuario o contraseña incorrectos"));
        return respuesta(cuenta);
    }

    public AuthResponse registrar(String username, String password) {
        String nombre = username.trim();
        if (usuarios.findByUsername(nombre).isPresent()) {
            throw ApiException.conflicto("El usuario ya existe");
        }
        UsuarioCuenta nueva = new UsuarioCuenta();
        nueva.setUsername(nombre);
        nueva.setPasswordHash(passwordEncoder.encode(password));
        nueva.setRol(Rol.FAN);
        nueva.setProveedor(Proveedor.LOCAL);
        return respuesta(usuarios.save(nueva));
    }

    /** Verifica el ID token de Google y emite el JWT propio de la API. */
    public AuthResponse loginGoogle(String idToken) {
        CuentaGoogle cuenta = googleTokenVerifier.verificar(idToken);
        UsuarioCuenta usuario = usuarios.findByProveedorAndProviderId(Proveedor.GOOGLE, cuenta.sub()).orElse(null);
        if (usuario == null) {
            // No se vincula automáticamente con una cuenta local del mismo nombre: evitaría
            // que alguien registre el correo de otra persona para quedarse con su cuenta.
            if (usuarios.findByUsername(cuenta.email()).isPresent()) {
                throw ApiException.conflicto("Ya existe una cuenta con el usuario " + cuenta.email()
                        + ". Inicia sesión con usuario y contraseña.");
            }
            usuario = new UsuarioCuenta();
            usuario.setUsername(cuenta.email());
            usuario.setEmail(cuenta.email());
            usuario.setRol(Rol.FAN);
            usuario.setProveedor(Proveedor.GOOGLE);
            usuario.setProviderId(cuenta.sub());
            usuario = usuarios.save(usuario);
        }
        return respuesta(usuario);
    }

    public UsuarioDto usuario(String username) {
        return usuarios.findByUsername(username).map(AuthService::dto)
                .orElseThrow(() -> new ApiException(CodigoError.NO_AUTENTICADO, "El usuario ya no existe"));
    }

    private AuthResponse respuesta(UsuarioCuenta cuenta) {
        UsuarioDto dto = dto(cuenta);
        TokenEmitido token = jwtService.emitir(dto.username(), dto.roles(), dto.proveedor());
        return new AuthResponse(token.valor(), "Bearer", token.expiraEn(), dto);
    }

    private static UsuarioDto dto(UsuarioCuenta c) {
        return new UsuarioDto(c.getUsername(), List.of(c.getRol().name()),
                c.getProveedor().name().toLowerCase(Locale.ROOT));
    }
}
