package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.TemporadaDtos.TemporadaDto;
import com.ligabeisbolcartagena.main.api.dto.TemporadaDtos.TemporadaRequest;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.dominio.Temporada;
import com.ligabeisbolcartagena.main.dominio.TemporadaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class TemporadaService {

    private final TemporadaRepository temporadas;
    private final Mapeador mapeador;

    public TemporadaService(TemporadaRepository temporadas, Mapeador mapeador) {
        this.temporadas = temporadas;
        this.mapeador = mapeador;
    }

    @Transactional(readOnly = true)
    public List<TemporadaDto> listar() {
        return temporadas.findAllByOrderByAnioDescIdDesc().stream().map(mapeador::dto).toList();
    }

    @Transactional(readOnly = true)
    public TemporadaDto actual() {
        return temporadas.findFirstByEstadoOrderByAnioDescIdDesc(Temporada.Estado.ACTIVE)
                .map(mapeador::dto)
                .orElseThrow(() -> ApiException.noEncontrado("Temporada activa", "actual"));
    }

    @Transactional(readOnly = true)
    public TemporadaDto obtener(Long id) {
        return mapeador.dto(buscar(id));
    }

    @Transactional
    public TemporadaDto crear(TemporadaRequest req) {
        validar(req, null);
        Temporada t = new Temporada();
        aplicar(t, req);
        return mapeador.dto(temporadas.save(t));
    }

    @Transactional
    public TemporadaDto actualizar(Long id, TemporadaRequest req) {
        Temporada t = buscar(id);
        validar(req, id);
        aplicar(t, req);
        return mapeador.dto(temporadas.save(t));
    }

    @Transactional
    public void eliminar(Long id) {
        Temporada t = buscar(id);
        try {
            temporadas.delete(t);
            temporadas.flush();
        } catch (DataIntegrityViolationException e) {
            throw Errores.relacionado();
        }
    }

    Temporada buscar(Long id) {
        return temporadas.findById(id).orElseThrow(() -> ApiException.noEncontrado("Temporada", String.valueOf(id)));
    }

    private void validar(TemporadaRequest req, Long id) {
        if (req.fechaInicio() != null && req.fechaFin() != null && req.fechaFin().isBefore(req.fechaInicio())) {
            throw ApiException.invalida("La fecha de fin no puede ser anterior a la de inicio",
                    Map.of("fechaFin", "anterior a fechaInicio"));
        }
        if (req.basesMuerteSubita() != null) {
            List<Integer> bases = req.basesMuerteSubita();
            if (bases.stream().anyMatch(b -> b == null || b < 1 || b > 3) || bases.stream().distinct().count() != bases.size()) {
                throw ApiException.invalida("Las bases de la muerte súbita deben ser primera (1), segunda (2) o tercera (3), sin repetir",
                        Map.of("basesMuerteSubita", "no válidas"));
            }
        }
        String nombre = req.nombre().trim();
        boolean repetida = id == null
                ? temporadas.existsByNombreAndAnio(nombre, req.anio())
                : temporadas.existsByNombreAndAnioAndIdNot(nombre, req.anio(), id);
        if (repetida) {
            throw ApiException.conflicto("Ya existe una temporada con ese nombre y año");
        }
    }

    private static void aplicar(Temporada t, TemporadaRequest req) {
        t.setNombre(req.nombre().trim());
        t.setAnio(req.anio());
        if (req.estado() != null) {
            t.setEstado(req.estado());
        }
        t.setFechaInicio(req.fechaInicio());
        t.setFechaFin(req.fechaFin());
        if (req.basesMuerteSubita() != null) {
            t.setBasesMuerteSubita(req.basesMuerteSubita().stream().sorted().map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(",")));
        }
    }
}
