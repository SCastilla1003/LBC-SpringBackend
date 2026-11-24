package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.UsuarioDtos.UsuarioAdminDto;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.api.error.CodigoError;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuenta;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuenta.Proveedor;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuenta.Rol;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

    UsuarioCuentaRepository repo;
    UsuarioService servicio;

    static UsuarioCuenta cuenta(long id, String username, Rol rol, Proveedor proveedor) {
        UsuarioCuenta c = new UsuarioCuenta();
        c.setId(id);
        c.setUsername(username);
        c.setRol(rol);
        c.setProveedor(proveedor);
        if (proveedor == Proveedor.GOOGLE) {
            c.setEmail(username);
        }
        return c;
    }

    @BeforeEach
    void preparar() {
        repo = mock(UsuarioCuentaRepository.class);
        servicio = new UsuarioService(repo);
        when(repo.save(any(UsuarioCuenta.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void listaCuentasLocalesYDeGoogle() {
        when(repo.findAll(any(Sort.class))).thenReturn(List.of(
                cuenta(2, "ana@gmail.com", Rol.FAN, Proveedor.GOOGLE),
                cuenta(1, "admin", Rol.ADMIN, Proveedor.LOCAL)));

        List<UsuarioAdminDto> r = servicio.listar();

        assertThat(r).extracting(UsuarioAdminDto::proveedor).containsExactly("google", "local");
        assertThat(r.get(0).email()).isEqualTo("ana@gmail.com");
        assertThat(r.get(1).rol()).isEqualTo("ADMIN");
    }

    @Test
    void cambiaElRolDeOtroUsuario() {
        UsuarioCuenta ana = cuenta(2, "ana@gmail.com", Rol.FAN, Proveedor.GOOGLE);
        when(repo.findById(2L)).thenReturn(Optional.of(ana));

        UsuarioAdminDto r = servicio.cambiarRol(2L, "ANOTADOR", "admin");

        assertThat(r.rol()).isEqualTo("ANOTADOR");
        assertThat(ana.getRol()).isEqualTo(Rol.ANOTADOR);
    }

    @Test
    void unAdminNoPuedeQuitarseSuPropioRol() {
        when(repo.findById(1L)).thenReturn(Optional.of(cuenta(1, "admin", Rol.ADMIN, Proveedor.LOCAL)));

        assertThatThrownBy(() -> servicio.cambiarRol(1L, "FAN", "admin"))
                .extracting(e -> ((ApiException) e).getCodigo())
                .isEqualTo(CodigoError.CONFLICTO);
        verify(repo, never()).save(any());
    }

    @Test
    void usuarioInexistente() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.cambiarRol(9L, "FAN", "admin"))
                .extracting(e -> ((ApiException) e).getCodigo())
                .isEqualTo(CodigoError.NO_ENCONTRADO);
    }
}
