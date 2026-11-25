package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.UsuarioDtos.UsuarioAdminDto;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuenta;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuenta.Rol;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuentaRepository;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Cuentas de la app (locales y de Google) y su rol. Solo para administradores. */
@Service
public class UsuarioService {

    private AuditoriaService auditoria = AuditoriaService.NINGUNA;

    @org.springframework.beans.factory.annotation.Autowired
    void setAuditoria(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    private final UsuarioCuentaRepository usuarios;

    public UsuarioService(UsuarioCuentaRepository usuarios) {
        this.usuarios = usuarios;
    }

    /** Todos los usuarios, los más recientes primero. */
    @Transactional(readOnly = true)
    public List<UsuarioAdminDto> listar() {
        return usuarios.findAll(Sort.by(Sort.Order.desc("creadoEn"), Sort.Order.desc("id")))
                .stream().map(UsuarioService::dto).toList();
    }

    /**
     * Cambia el rol de un usuario. Un administrador no puede cambiarse el suyo: así
     * nunca se queda la liga sin administradores por accidente.
     */
    @Transactional
    public UsuarioAdminDto cambiarRol(Long id, String rol, String solicitante) {
        UsuarioCuenta cuenta = usuarios.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("Usuario", String.valueOf(id)));
        Rol nuevo = Rol.valueOf(rol);
        if (cuenta.getUsername().equals(solicitante) && nuevo != cuenta.getRol()) {
            throw ApiException.conflicto("No puedes cambiar tu propio rol. Pídele a otro administrador que lo haga.");
        }
        String rolAntes = cuenta.getRol().name();
        cuenta.setRol(nuevo);
        var dto = dto(usuarios.save(cuenta));
        if (!rolAntes.equals(nuevo.name())) {
            auditoria.registrar("CAMBIAR_ROL", "Usuario", id,
                    "Cambió el rol de " + cuenta.getUsername() + " de " + rolAntes + " a " + nuevo.name(),
                    java.util.Map.of("rol", rolAntes), java.util.Map.of("rol", nuevo.name()));
        }
        return dto;
    }

    static UsuarioAdminDto dto(UsuarioCuenta c) {
        return new UsuarioAdminDto(c.getId(), c.getUsername(), c.getEmail(), c.getRol().name(),
                c.getProveedor().name().toLowerCase(Locale.ROOT), c.isActiva(), c.getCreadoEn());
    }
}
