package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.EstadioDtos.EstadioDto;
import com.ligabeisbolcartagena.main.api.dto.EstadioDtos.EstadioRequest;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.dominio.Estadio;
import com.ligabeisbolcartagena.main.dominio.EstadioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class EstadioService {

    private AuditoriaService auditoria = AuditoriaService.NINGUNA;

    @org.springframework.beans.factory.annotation.Autowired
    void setAuditoria(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    private final EstadioRepository estadios;
    private final Mapeador mapeador;

    public EstadioService(EstadioRepository estadios, Mapeador mapeador) {
        this.estadios = estadios;
        this.mapeador = mapeador;
    }

    @Transactional(readOnly = true)
    public List<EstadioDto> listar() {
        return estadios.findAllByOrderByNombreAsc().stream().map(mapeador::dto).toList();
    }

    @Transactional(readOnly = true)
    public EstadioDto obtener(Long id) {
        return mapeador.dto(buscar(id));
    }

    @Transactional
    public EstadioDto crear(EstadioRequest req) {
        Estadio e = new Estadio();
        aplicar(e, req, null);
        var dto = mapeador.dto(estadios.save(e));
        auditoria.registrar("CREAR", "Estadio", dto.id(), "Creó el estadio " + dto.nombre(), null, dto);
        return dto;
    }

    @Transactional
    public EstadioDto actualizar(Long id, EstadioRequest req) {
        Estadio e = buscar(id);
        var antes = mapeador.dto(e);
        aplicar(e, req, id);
        var dto = mapeador.dto(estadios.save(e));
        auditoria.registrar("EDITAR", "Estadio", id, "Editó el estadio " + dto.nombre(), antes, dto);
        return dto;
    }

    @Transactional
    public void eliminar(Long id) {
        Estadio e = buscar(id);
        var antes = mapeador.dto(e);
        try {
            estadios.delete(e);
            estadios.flush();
            auditoria.registrar("ELIMINAR", "Estadio", id, "Eliminó el estadio " + antes.nombre(), antes, null);
        } catch (DataIntegrityViolationException ex) {
            throw Errores.relacionado();
        }
    }

    Estadio buscar(Long id) {
        return estadios.findById(id).orElseThrow(() -> ApiException.noEncontrado("Estadio", String.valueOf(id)));
    }

    private void aplicar(Estadio e, EstadioRequest req, Long id) {
        String nombre = req.nombre().trim();
        boolean repetido = id == null ? estadios.existsByNombre(nombre) : estadios.existsByNombreAndIdNot(nombre, id);
        if (repetido) {
            throw ApiException.invalida("Ya existe un estadio con ese nombre", Map.of("nombre", "repetido"));
        }
        e.setNombre(nombre);
        e.setUbicacion(req.ubicacion());
    }
}
