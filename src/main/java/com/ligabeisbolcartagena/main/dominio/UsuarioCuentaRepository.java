package com.ligabeisbolcartagena.main.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioCuentaRepository extends JpaRepository<UsuarioCuenta, Long> {

    Optional<UsuarioCuenta> findByUsername(String username);

    Optional<UsuarioCuenta> findByProveedorAndProviderId(UsuarioCuenta.Proveedor proveedor, String providerId);
}
