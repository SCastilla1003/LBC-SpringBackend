package com.ligabeisbolcartagena.main.config;

import com.ligabeisbolcartagena.main.dominio.UsuarioCuenta;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuenta.Rol;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuentaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Garantiza el usuario administrador. La contraseña viene de ADMIN_PASSWORD:
 * si está definida y no coincide con la guardada, se actualiza. Si no está definida,
 * no se crea ni se modifica nada.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    @Order(2)
    CommandLineRunner initUsuarios(UsuarioCuentaRepository usuarios, PasswordEncoder encoder,
                                   @Value("${lbc.admin.usuario:adminlbc}") String usuarioAdmin,
                                   @Value("${lbc.admin.password:}") String passwordAdmin) {
        return args -> {
            if (passwordAdmin == null || passwordAdmin.isBlank()) {
                log.warn("ADMIN_PASSWORD no está definida: no se crea ni actualiza el usuario '{}'.", usuarioAdmin);
                return;
            }
            UsuarioCuenta admin = usuarios.findByUsername(usuarioAdmin).orElse(null);
            if (admin == null) {
                admin = new UsuarioCuenta();
                admin.setUsername(usuarioAdmin);
                admin.setPasswordHash(encoder.encode(passwordAdmin));
                admin.setRol(Rol.ADMIN);
                usuarios.save(admin);
                log.info("Usuario administrador '{}' creado.", usuarioAdmin);
                return;
            }
            boolean cambios = false;
            if (admin.getPasswordHash() == null || !encoder.matches(passwordAdmin, admin.getPasswordHash())) {
                admin.setPasswordHash(encoder.encode(passwordAdmin));
                cambios = true;
            }
            if (admin.getRol() != Rol.ADMIN) {
                admin.setRol(Rol.ADMIN);
                cambios = true;
            }
            if (cambios) {
                usuarios.save(admin);
                log.info("Usuario administrador '{}' actualizado con ADMIN_PASSWORD.", usuarioAdmin);
            }
        };
    }
}
