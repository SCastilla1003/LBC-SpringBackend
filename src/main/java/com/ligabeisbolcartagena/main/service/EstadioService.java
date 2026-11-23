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
        return mapeador.dto(estadios.save(e));
    }

    @Transactional
    public EstadioDto actualizar(Long id, EstadioRequest req) {
        Estadio e = buscar(id);
        aplicar(e, req, id);
        return mapeador.dto(estadios.save(e));
    }

    @Transactional
    public void eliminar(Long id) {
        Estadio e = buscar(id);
        try {
            estadios.delete(e);
            estadios.flush();
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
