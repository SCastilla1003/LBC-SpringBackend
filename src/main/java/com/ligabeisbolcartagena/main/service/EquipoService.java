package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.ComunDtos.EquipoResumen;
import com.ligabeisbolcartagena.main.api.dto.EquipoDtos.EquipoDto;
import com.ligabeisbolcartagena.main.api.dto.EquipoDtos.EquipoRequest;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.dominio.Equipo;
import com.ligabeisbolcartagena.main.dominio.EquipoRepository;
import com.ligabeisbolcartagena.main.dominio.PlantillaRepository;
import com.ligabeisbolcartagena.main.dominio.TemporadaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class EquipoService {

    private final EquipoRepository equipos;
    private final PlantillaRepository plantillas;
    private final TemporadaRepository temporadas;
    private final Mapeador mapeador;

    public EquipoService(EquipoRepository equipos, PlantillaRepository plantillas, TemporadaRepository temporadas,
                         Mapeador mapeador) {
        this.equipos = equipos;
        this.plantillas = plantillas;
        this.temporadas = temporadas;
        this.mapeador = mapeador;
    }

    @Transactional(readOnly = true)
    public List<EquipoDto> listar() {
        return equipos.findAllByOrderByNombreAsc().stream().map(mapeador::dto).toList();
    }

    @Transactional(readOnly = true)
    public EquipoDto obtener(Long id) {
        return mapeador.dto(buscar(id));
    }

    @Transactional
    public EquipoDto crear(EquipoRequest req) {
        Equipo e = new Equipo();
        aplicar(e, req, null);
        return mapeador.dto(equipos.save(e));
    }

    @Transactional
    public EquipoDto actualizar(Long id, EquipoRequest req) {
        Equipo e = buscar(id);
        aplicar(e, req, id);
        return mapeador.dto(equipos.save(e));
    }

    @Transactional
    public void eliminar(Long id) {
        Equipo e = buscar(id);
        try {
            equipos.delete(e);
            equipos.flush();
        } catch (DataIntegrityViolationException ex) {
            throw Errores.relacionado();
        }
    }

    /** Equipos que tienen jugadores inscritos en la temporada. */
    @Transactional(readOnly = true)
    public List<EquipoResumen> deTemporada(Long temporadaId) {
        if (!temporadas.existsById(temporadaId)) {
            throw ApiException.noEncontrado("Temporada", String.valueOf(temporadaId));
        }
        return plantillas.findByTemporadaId(temporadaId).stream()
                .map(p -> p.getEquipo())
                .filter(e -> e != null)
                .distinct()
                .sorted(Comparator.comparing(Equipo::getNombre))
                .map(mapeador::res)
                .toList();
    }

    Equipo buscar(Long id) {
        return equipos.findById(id).orElseThrow(() -> ApiException.noEncontrado("Equipo", String.valueOf(id)));
    }

    private void aplicar(Equipo e, EquipoRequest req, Long id) {
        String nombre = req.nombre().trim();
        String abreviatura = req.abreviatura().trim().toUpperCase();
        boolean nombreRepetido = id == null ? equipos.existsByNombre(nombre) : equipos.existsByNombreAndIdNot(nombre, id);
        if (nombreRepetido) {
            throw ApiException.invalida("Ya existe un equipo con ese nombre", Map.of("nombre", "repetido"));
        }
        boolean abreviaturaRepetida = id == null ? equipos.existsByAbreviatura(abreviatura)
                : equipos.existsByAbreviaturaAndIdNot(abreviatura, id);
        if (abreviaturaRepetida) {
            throw ApiException.invalida("Ya existe un equipo con esa abreviatura",
                    Map.of("abreviatura", "repetida"));
        }
        e.setNombre(nombre);
        e.setAbreviatura(abreviatura);
        e.setCiudad(req.ciudad());
        e.setColor(req.color());
        e.setEscudoUrl(req.escudoUrl());
    }
}
