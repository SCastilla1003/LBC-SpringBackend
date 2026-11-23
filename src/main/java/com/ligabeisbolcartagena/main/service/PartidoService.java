package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.ComunDtos.EquipoResumen;
import com.ligabeisbolcartagena.main.api.dto.PartidoDtos.CorredoresDto;
import com.ligabeisbolcartagena.main.api.dto.PartidoDtos.ParLocalVisita;
import com.ligabeisbolcartagena.main.api.dto.PartidoDtos.PartidoActualizacion;
import com.ligabeisbolcartagena.main.api.dto.PartidoDtos.PartidoDetalleDto;
import com.ligabeisbolcartagena.main.api.dto.PartidoDtos.PartidoRequest;
import com.ligabeisbolcartagena.main.api.dto.PartidoDtos.PartidoResumenDto;
import com.ligabeisbolcartagena.main.api.dto.PartidoDtos.PosicionDto;
import com.ligabeisbolcartagena.main.api.dto.PartidoDtos.SituacionDto;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.dominio.Equipo;
import com.ligabeisbolcartagena.main.dominio.EquipoRepository;
import com.ligabeisbolcartagena.main.dominio.Estadio;
import com.ligabeisbolcartagena.main.dominio.EstadioRepository;
import com.ligabeisbolcartagena.main.dominio.JugadaRepository;
import com.ligabeisbolcartagena.main.dominio.Mitad;
import com.ligabeisbolcartagena.main.dominio.Partido;
import com.ligabeisbolcartagena.main.dominio.PartidoRepository;
import com.ligabeisbolcartagena.main.dominio.Temporada;
import com.ligabeisbolcartagena.main.dominio.TemporadaRepository;
import com.ligabeisbolcartagena.main.service.LineScoreBuilder.CarreraMitad;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PartidoService {

    private final PartidoRepository partidos;
    private final TemporadaRepository temporadas;
    private final EquipoRepository equipos;
    private final EstadioRepository estadios;
    private final JugadaRepository jugadas;
    private final Mapeador mapeador;

    public PartidoService(PartidoRepository partidos, TemporadaRepository temporadas, EquipoRepository equipos,
                          EstadioRepository estadios, JugadaRepository jugadas, Mapeador mapeador) {
        this.partidos = partidos;
        this.temporadas = temporadas;
        this.equipos = equipos;
        this.estadios = estadios;
        this.jugadas = jugadas;
        this.mapeador = mapeador;
    }

    @Transactional(readOnly = true)
    public List<PartidoResumenDto> listar(Long temporadaId, Partido.Estado estado, Long equipoId, LocalDate desde,
                                          LocalDate hasta) {
        validarTemporada(temporadaId);
        return partidos.findByTemporadaIdOrderByFechaHoraAsc(temporadaId).stream()
                .filter(p -> estado == null || p.getEstado() == estado)
                .filter(p -> equipoId == null || p.getEquipoLocal().getId().equals(equipoId)
                        || p.getEquipoVisita().getId().equals(equipoId))
                .filter(p -> desde == null || !p.getFechaHora().toLocalDate().isBefore(desde))
                .filter(p -> hasta == null || !p.getFechaHora().toLocalDate().isAfter(hasta))
                .map(this::resumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public PartidoDetalleDto obtener(Long id) {
        return detalle(buscar(id));
    }

    @Transactional
    public PartidoDetalleDto crear(Long temporadaId, PartidoRequest req) {
        Temporada temporada = validarTemporada(temporadaId);
        if (req.equipoLocalId().equals(req.equipoVisitaId())) {
            throw ApiException.invalida("El equipo local y el visitante deben ser distintos",
                    Map.of("equipoVisitaId", "igual al local"));
        }
        Partido p = new Partido();
        p.setTemporada(temporada);
        p.setEquipoLocal(equipo(req.equipoLocalId()));
        p.setEquipoVisita(equipo(req.equipoVisitaId()));
        p.setFechaHora(req.fechaHora());
        p.setEstadio(estadioOpcional(req.estadioId()));
        p.setEntradasProgramadas(req.entradasProgramadas() == null ? 9 : req.entradasProgramadas());
        p.setEstado(Partido.Estado.SCHEDULED);
        return detalle(partidos.save(p));
    }

    @Transactional
    public PartidoDetalleDto actualizar(Long id, PartidoActualizacion req) {
        Partido p = buscar(id);
        if (p.getEstado() != Partido.Estado.SCHEDULED && p.getEstado() != Partido.Estado.LINEUP_PUBLISHED) {
            throw ApiException.conflicto("El partido ya empezó o terminó; no se puede editar");
        }
        if (req.fechaHora() != null) {
            p.setFechaHora(req.fechaHora());
        }
        if (req.estadioId() != null) {
            p.setEstadio(estadioOpcional(req.estadioId()));
        }
        if (req.entradasProgramadas() != null) {
            p.setEntradasProgramadas(req.entradasProgramadas());
        }
        return detalle(partidos.save(p));
    }

    @Transactional
    public void eliminar(Long id) {
        Partido p = buscar(id);
        if (p.getEstado() != Partido.Estado.SCHEDULED) {
            throw ApiException.conflicto("Solo se pueden borrar partidos programados");
        }
        try {
            partidos.delete(p);
            partidos.flush();
        } catch (DataIntegrityViolationException e) {
            throw Errores.relacionado();
        }
    }

    /** Tabla de posiciones calculada con los partidos finalizados de la temporada. */
    @Transactional(readOnly = true)
    public List<PosicionDto> posiciones(Long temporadaId) {
        validarTemporada(temporadaId);
        Map<Long, Acumulado> acumulados = new HashMap<>();
        Map<Long, Equipo> porId = new HashMap<>();
        for (Partido p : partidos.findByTemporadaIdOrderByFechaHoraAsc(temporadaId)) {
            porId.put(p.getEquipoLocal().getId(), p.getEquipoLocal());
            porId.put(p.getEquipoVisita().getId(), p.getEquipoVisita());
            acumulados.computeIfAbsent(p.getEquipoLocal().getId(), k -> new Acumulado());
            acumulados.computeIfAbsent(p.getEquipoVisita().getId(), k -> new Acumulado());
        }
        for (Partido p : partidos.findByTemporadaIdAndEstado(temporadaId, Partido.Estado.FINISHED)) {
            Acumulado local = acumulados.get(p.getEquipoLocal().getId());
            Acumulado visita = acumulados.get(p.getEquipoVisita().getId());
            local.registrar(p.getCarrerasLocal(), p.getCarrerasVisita());
            visita.registrar(p.getCarrerasVisita(), p.getCarrerasLocal());
        }
        List<PosicionDto> tabla = new ArrayList<>();
        for (Map.Entry<Long, Acumulado> e : acumulados.entrySet()) {
            Acumulado a = e.getValue();
            tabla.add(new PosicionDto(mapeador.res(porId.get(e.getKey())), a.juegos, a.ganados, a.perdidos,
                    a.pct(), a.favor, a.contra));
        }
        tabla.sort(Comparator.comparing(PosicionDto::pct).reversed()
                .thenComparing(p -> p.carrerasFavor() - p.carrerasContra(), Comparator.reverseOrder())
                .thenComparing(p -> p.equipo().nombre()));
        return tabla;
    }

    Partido buscar(Long id) {
        return partidos.findById(id).orElseThrow(() -> ApiException.noEncontrado("Partido", String.valueOf(id)));
    }

    private Temporada validarTemporada(Long temporadaId) {
        return temporadas.findById(temporadaId)
                .orElseThrow(() -> ApiException.noEncontrado("Temporada", String.valueOf(temporadaId)));
    }

    private Equipo equipo(Long id) {
        return equipos.findById(id).orElseThrow(() -> ApiException.noEncontrado("Equipo", String.valueOf(id)));
    }

    private Estadio estadioOpcional(Long id) {
        if (id == null) {
            return null;
        }
        return estadios.findById(id).orElseThrow(() -> ApiException.noEncontrado("Estadio", String.valueOf(id)));
    }

    private PartidoResumenDto resumen(Partido p) {
        return new PartidoResumenDto(p.getId(), p.getTemporada().getId(), p.getFechaHora(), p.getEstado(),
                p.getEstadio() == null ? null : mapeador.dto(p.getEstadio()),
                mapeador.res(p.getEquipoLocal()), mapeador.res(p.getEquipoVisita()),
                new ParLocalVisita(p.getCarrerasLocal(), p.getCarrerasVisita()), ganadorId(p));
    }

    private PartidoDetalleDto detalle(Partido p) {
        List<CarreraMitad> carreras = new ArrayList<>();
        for (Object[] fila : jugadas.carrerasPorEntrada(p.getId())) {
            carreras.add(new CarreraMitad(((Number) fila[0]).intValue(), (Mitad) fila[1],
                    ((Number) fila[2]).intValue()));
        }
        return new PartidoDetalleDto(p.getId(), p.getTemporada().getId(), p.getFechaHora(), p.getEstado(),
                p.getEstadio() == null ? null : mapeador.dto(p.getEstadio()),
                mapeador.res(p.getEquipoLocal()), mapeador.res(p.getEquipoVisita()),
                new ParLocalVisita(p.getCarrerasLocal(), p.getCarrerasVisita()), ganadorId(p),
                p.getEntradasProgramadas(), p.getEntradaActual(), p.getMitad(),
                new SituacionDto(p.getOuts(), p.getBolas(), p.getStrikes(), mapeador.ref(p.getBateador()),
                        mapeador.ref(p.getLanzador()),
                        new CorredoresDto(mapeador.ref(p.getCorredor1b()), mapeador.ref(p.getCorredor2b()),
                                mapeador.ref(p.getCorredor3b()))),
                new ParLocalVisita(p.getHitsLocal(), p.getHitsVisita()),
                new ParLocalVisita(p.getErroresLocal(), p.getErroresVisita()),
                LineScoreBuilder.construir(p.getEntradasProgramadas(), p.getEstado(), p.getEntradaActual(),
                        p.getMitad(), carreras),
                p.getVersion());
    }

    private static Long ganadorId(Partido p) {
        if (p.getEstado() != Partido.Estado.FINISHED || p.getCarrerasLocal() == p.getCarrerasVisita()) {
            return null;
        }
        return p.getCarrerasLocal() > p.getCarrerasVisita() ? p.getEquipoLocal().getId() : p.getEquipoVisita().getId();
    }

    private static final class Acumulado {
        int juegos;
        int ganados;
        int perdidos;
        int favor;
        int contra;

        void registrar(int anotadas, int recibidas) {
            juegos++;
            if (anotadas > recibidas) {
                ganados++;
            } else {
                perdidos++;
            }
            favor += anotadas;
            contra += recibidas;
        }

        BigDecimal pct() {
            if (juegos == 0) {
                return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
            }
            return BigDecimal.valueOf(ganados).divide(BigDecimal.valueOf(juegos), 3, RoundingMode.HALF_UP);
        }
    }
}
