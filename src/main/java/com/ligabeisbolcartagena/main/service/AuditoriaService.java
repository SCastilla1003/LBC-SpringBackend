package com.ligabeisbolcartagena.main.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ligabeisbolcartagena.main.api.dto.AuditoriaDtos.PaginaAuditoria;
import com.ligabeisbolcartagena.main.api.dto.AuditoriaDtos.RegistroDto;
import com.ligabeisbolcartagena.main.dominio.RegistroAuditoria;
import com.ligabeisbolcartagena.main.dominio.RegistroAuditoriaRepository;
import com.ligabeisbolcartagena.main.dominio.UsuarioCuentaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Registro de auditoría: quién cambió qué y cuándo (crear, editar, mover, eliminar...). Cada
 * servicio lo llama después de un cambio que salió bien, dentro de la misma transacción: si
 * el cambio se deshace, el registro también.
 */
@Service
public class AuditoriaService {

    /** Para pruebas y servicios creados a mano: no registra nada. */
    public static final AuditoriaService NINGUNA = new AuditoriaService(null, null, null);

    private final RegistroAuditoriaRepository registros;
    private final UsuarioCuentaRepository usuarios;
    private final ObjectMapper json;

    public AuditoriaService(RegistroAuditoriaRepository registros, UsuarioCuentaRepository usuarios,
                            ObjectMapper json) {
        this.registros = registros;
        this.usuarios = usuarios;
        this.json = json;
    }

    /**
     * Anota un cambio. [antes] y [despues] son los datos (DTO) del objeto antes y después; pueden
     * ser nulos (al crear no hay "antes"; al eliminar no hay "después").
     */
    public void registrar(String accion, String entidad, Long entidadId, String descripcion, Object antes,
                          Object despues) {
        if (registros == null) {
            return;
        }
        RegistroAuditoria r = new RegistroAuditoria();
        r.setAccion(accion);
        r.setEntidad(entidad);
        r.setEntidadId(entidadId);
        r.setDescripcion(descripcion.length() > 400 ? descripcion.substring(0, 397) + "..." : descripcion);
        r.setAntes(aJson(antes));
        r.setDespues(aJson(despues));
        String usuario = usuarioActual();
        r.setUsuario(usuario);
        if (usuario != null) {
            r.setAdmin(usuarios.findByUsername(usuario).orElse(null));
        }
        registros.save(r);
    }

    /** Lo más reciente primero. [entidad] y [texto] filtran (nulos = todo). */
    @Transactional(readOnly = true)
    public PaginaAuditoria listar(String entidad, String texto, int pagina, int tamano) {
        int t = Math.max(1, Math.min(tamano, 100));
        String filtroTexto = texto == null || texto.isBlank() ? null : texto.trim().toLowerCase();
        String filtroEntidad = entidad == null || entidad.isBlank() ? null : entidad;
        int p = Math.max(0, pagina);
        List<RegistroAuditoria> filas = registros.buscar(filtroEntidad, filtroTexto, PageRequest.of(p, t));
        boolean hayMas = (long) (p + 1) * t < registros.contar(filtroEntidad, filtroTexto);
        return new PaginaAuditoria(
                filas.stream().map(r -> new RegistroDto(r.getId(), r.getFecha(), r.getUsuario(),
                        r.getAccion(), r.getEntidad(), r.getEntidadId(), r.getDescripcion(), r.getAntes(),
                        r.getDespues())).toList(),
                p, hayMas);
    }

    private String aJson(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return json.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String usuarioActual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getName()) ? null : a.getName();
    }

    /** Nombre para los textos: "Giancarlo Stanton". */
    static String nombre(String nombres, String apellidos) {
        return (nombres + " " + apellidos).trim();
    }
}
