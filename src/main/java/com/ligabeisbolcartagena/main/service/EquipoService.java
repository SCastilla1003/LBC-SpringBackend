package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.ComunDtos.EquipoResumen;
import com.ligabeisbolcartagena.main.api.dto.EquipoDtos.EquipoDto;
import com.ligabeisbolcartagena.main.api.dto.EquipoDtos.EquipoRequest;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.dominio.Equipo;
import com.ligabeisbolcartagena.main.dominio.EquipoRepository;
import com.ligabeisbolcartagena.main.dominio.InscripcionEquipo;
import com.ligabeisbolcartagena.main.dominio.InscripcionEquipoRepository;
import com.ligabeisbolcartagena.main.dominio.PartidoRepository;
import com.ligabeisbolcartagena.main.dominio.PlantillaRepository;
import com.ligabeisbolcartagena.main.dominio.Temporada;
import com.ligabeisbolcartagena.main.dominio.TemporadaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class EquipoService {

    private AuditoriaService auditoria = AuditoriaService.NINGUNA;

    @org.springframework.beans.factory.annotation.Autowired
    void setAuditoria(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    private final EquipoRepository equipos;
    private final PlantillaRepository plantillas;
    private final TemporadaRepository temporadas;
    private final InscripcionEquipoRepository inscripciones;
    private final PartidoRepository partidos;
    private final Mapeador mapeador;

    public EquipoService(EquipoRepository equipos, PlantillaRepository plantillas, TemporadaRepository temporadas,
                         InscripcionEquipoRepository inscripciones, PartidoRepository partidos, Mapeador mapeador) {
        this.equipos = equipos;
        this.plantillas = plantillas;
        this.temporadas = temporadas;
        this.inscripciones = inscripciones;
        this.partidos = partidos;
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
        var dto = mapeador.dto(equipos.save(e));
        auditoria.registrar("CREAR", "Equipo", dto.id(), "Creó el equipo " + dto.nombre(), null, dto);
        return dto;
    }

    @Transactional
    public EquipoDto actualizar(Long id, EquipoRequest req) {
        Equipo e = buscar(id);
        var antes = mapeador.dto(e);
        aplicar(e, req, id);
        var dto = mapeador.dto(equipos.save(e));
        auditoria.registrar("EDITAR", "Equipo", id, "Editó el equipo " + dto.nombre(), antes, dto);
        return dto;
    }

    @Transactional
    public void eliminar(Long id) {
        Equipo e = buscar(id);
        var antes = mapeador.dto(e);
        try {
            equipos.delete(e);
            equipos.flush();
            auditoria.registrar("ELIMINAR", "Equipo", id, "Eliminó el equipo " + antes.nombre(), antes, null);
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
        // Inscritos explícitamente + los que tienen jugadores en la plantilla.
        return Stream.concat(
                        inscripciones.findByTemporadaId(temporadaId).stream().map(InscripcionEquipo::getEquipo),
                        plantillas.findByTemporadaId(temporadaId).stream().map(p -> p.getEquipo()))
                .filter(e -> e != null)
                .distinct()
                .sorted(Comparator.comparing(Equipo::getNombre))
                .map(mapeador::res)
                .toList();
    }

    /** Inscribe un equipo en la temporada, aunque todavía no tenga jugadores. */
    @Transactional
    public EquipoResumen inscribir(Long temporadaId, Long equipoId) {
        Temporada t = temporadas.findById(temporadaId)
                .orElseThrow(() -> ApiException.noEncontrado("Temporada", String.valueOf(temporadaId)));
        Equipo e = buscar(equipoId);
        if (inscripciones.existsByTemporadaIdAndEquipoId(temporadaId, equipoId)) {
            throw ApiException.conflicto(e.getNombre() + " ya está inscrito en esta temporada");
        }
        inscripciones.save(new InscripcionEquipo(t, e));
        auditoria.registrar("INSCRIBIR", "Equipo", equipoId,
                "Inscribió a " + e.getNombre() + " en la temporada " + t.getNombre() + " " + t.getAnio(), null, null);
        return mapeador.res(e);
    }

    /** Quita un equipo de la temporada. Solo si no tiene jugadores ni partidos en ella. */
    @Transactional
    public void desinscribir(Long temporadaId, Long equipoId) {
        Equipo e = buscar(equipoId);
        if (!plantillas.findByTemporadaIdAndEquipoId(temporadaId, equipoId).isEmpty()) {
            throw ApiException.conflicto(e.getNombre() + " tiene jugadores en esta temporada; quítalos primero del roster");
        }
        boolean juega = partidos.findByTemporadaIdOrderByFechaHoraAsc(temporadaId).stream()
                .anyMatch(p -> equipoId.equals(p.getEquipoLocal().getId()) || equipoId.equals(p.getEquipoVisita().getId()));
        if (juega) {
            throw ApiException.conflicto(e.getNombre() + " tiene partidos en esta temporada");
        }
        inscripciones.findByTemporadaIdAndEquipoId(temporadaId, equipoId).ifPresent(inscripciones::delete);
        auditoria.registrar("DESINSCRIBIR", "Equipo", equipoId,
                "Sacó a " + e.getNombre() + " de la temporada " + temporadaId, null, null);
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
