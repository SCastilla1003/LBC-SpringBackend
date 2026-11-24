package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.ComunDtos.EquipoResumen;
import com.ligabeisbolcartagena.main.api.dto.EstadisticasDtos.BateoDto;
import com.ligabeisbolcartagena.main.api.dto.EstadisticasDtos.BloqueDto;
import com.ligabeisbolcartagena.main.api.dto.EstadisticasDtos.EnfrentamientoDto;
import com.ligabeisbolcartagena.main.api.dto.EstadisticasDtos.EstadisticasJugadorDto;
import com.ligabeisbolcartagena.main.api.dto.EstadisticasDtos.FilaBateoDto;
import com.ligabeisbolcartagena.main.api.dto.EstadisticasDtos.FilaPitcheoDto;
import com.ligabeisbolcartagena.main.api.dto.EstadisticasDtos.PitcheoDto;
import com.ligabeisbolcartagena.main.api.dto.EstadisticasDtos.PronosticoDto;
import com.ligabeisbolcartagena.main.api.dto.EstadisticasDtos.TurnoHistorialDto;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.dominio.Jugada.Resultado;
import com.ligabeisbolcartagena.main.dominio.JugadaRepository;
import com.ligabeisbolcartagena.main.dominio.Jugador;
import com.ligabeisbolcartagena.main.dominio.JugadorRepository;
import com.ligabeisbolcartagena.main.dominio.Mitad;
import com.ligabeisbolcartagena.main.dominio.Partido;
import com.ligabeisbolcartagena.main.dominio.Plantilla;
import com.ligabeisbolcartagena.main.dominio.PlantillaRepository;
import com.ligabeisbolcartagena.main.dominio.TemporadaRepository;
import com.ligabeisbolcartagena.main.service.estadisticas.CalculadoraEstadisticas;
import com.ligabeisbolcartagena.main.service.estadisticas.CalculadoraEstadisticas.Bateo;
import com.ligabeisbolcartagena.main.service.estadisticas.CalculadoraEstadisticas.Calculo;
import com.ligabeisbolcartagena.main.service.estadisticas.CalculadoraEstadisticas.FilaJugada;
import com.ligabeisbolcartagena.main.service.estadisticas.CalculadoraEstadisticas.FilaMovimiento;
import com.ligabeisbolcartagena.main.service.estadisticas.CalculadoraEstadisticas.Pitcheo;
import com.ligabeisbolcartagena.main.service.estadisticas.Pronostico;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Estadísticas calculadas en el momento con las jugadas del anotador (incluye partidos en vivo). */
@Service
public class EstadisticasService {

    private final JugadaRepository jugadas;
    private final JugadorRepository jugadores;
    private final PlantillaRepository plantillas;
    private final TemporadaRepository temporadas;
    private final Mapeador mapeador;

    public EstadisticasService(JugadaRepository jugadas, JugadorRepository jugadores, PlantillaRepository plantillas,
                               TemporadaRepository temporadas, Mapeador mapeador) {
        this.jugadas = jugadas;
        this.jugadores = jugadores;
        this.plantillas = plantillas;
        this.temporadas = temporadas;
        this.mapeador = mapeador;
    }

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public EstadisticasJugadorDto deJugador(Long temporadaId, Long jugadorId) {
        validarTemporada(temporadaId);
        Jugador j = jugador(jugadorId);
        Calculo temporada = calcular(temporadaId);
        Calculo carrera = calcular(null);
        return new EstadisticasJugadorDto(mapeador.res(j), bloque(temporada, jugadorId), bloque(carrera, jugadorId));
    }

    /** Todos los que han bateado en la temporada. */
    @Transactional(readOnly = true)
    public List<FilaBateoDto> bateo(Long temporadaId) {
        validarTemporada(temporadaId);
        Calculo c = calcular(temporadaId);
        Map<Long, Jugador> porId = jugadores(c.bateo().keySet());
        Map<Long, EquipoResumen> equipos = equipos(temporadaId);
        List<FilaBateoDto> filas = new ArrayList<>();
        c.bateo().forEach((id, b) -> {
            if (b.pa > 0 && porId.containsKey(id)) {
                filas.add(new FilaBateoDto(mapeador.res(porId.get(id)), equipos.get(id), dto(b)));
            }
        });
        filas.sort(Comparator.comparing((FilaBateoDto f) -> f.bateo().ops()).reversed());
        return filas;
    }

    /** Todos los que han lanzado en la temporada. */
    @Transactional(readOnly = true)
    public List<FilaPitcheoDto> pitcheo(Long temporadaId) {
        validarTemporada(temporadaId);
        Calculo c = calcular(temporadaId);
        Map<Long, Jugador> porId = jugadores(c.pitcheo().keySet());
        Map<Long, EquipoResumen> equipos = equipos(temporadaId);
        List<FilaPitcheoDto> filas = new ArrayList<>();
        c.pitcheo().forEach((id, p) -> {
            if (porId.containsKey(id)) {
                filas.add(new FilaPitcheoDto(mapeador.res(porId.get(id)), equipos.get(id), dto(p)));
            }
        });
        filas.sort(Comparator.comparingInt((FilaPitcheoDto f) -> f.pitcheo().outs()).reversed());
        return filas;
    }

    /**
     * Historial del bateador contra el lanzador (todas las temporadas) y pronóstico para su
     * próximo turno con los números de [temporadaId] (o de toda la carrera si es nulo).
     */
    @Transactional(readOnly = true)
    public EnfrentamientoDto enfrentamiento(Long bateadorId, Long lanzadorId, Long temporadaId) {
        if (temporadaId != null) {
            validarTemporada(temporadaId);
        }
        Jugador bateador = jugador(bateadorId);
        Jugador lanzador = jugador(lanzadorId);
        if (bateadorId.equals(lanzadorId)) {
            throw ApiException.invalida("Elige dos jugadores distintos");
        }

        List<FilaJugada> todas = filas(null);
        List<FilaJugada> directas = todas.stream()
                .filter(f -> f.esTurno() && f.bateadorId() == bateadorId && f.lanzadorId() == lanzadorId)
                .sorted(Comparator.comparing(FilaJugada::fecha).thenComparingInt(FilaJugada::secuencia).reversed())
                .toList();
        Bateo historial = CalculadoraEstadisticas.calcular(directas, List.of(), Map.of()).bateoDe(bateadorId);

        Calculo base = temporadaId == null
                ? CalculadoraEstadisticas.calcular(todas, List.of(), Map.of())
                : calcular(temporadaId);
        Bateo b = base.bateoDe(bateadorId);
        Pitcheo p = base.pitcheoDe(lanzadorId);
        Bateo liga = new Bateo();
        base.bateo().values().forEach(x -> sumarA(liga, x));
        Pronostico.Resultado r = Pronostico.calcular(b, p, historial, Pronostico.Liga.de(liga));

        String nombreB = bateador.getNombres() + " " + bateador.getApellidos();
        String nombreP = lanzador.getNombres() + " " + lanzador.getApellidos();
        int turnosB = b == null ? 0 : b.pa;
        int bateadoresP = p == null ? 0 : p.bateadores;
        int directos = historial == null ? 0 : historial.pa;
        String resumen = "Estimado con " + turnosB + " apariciones al plato de " + nombreB + ", "
                + bateadoresP + " bateadores enfrentados por " + nombreP
                + (directos == 0 ? " y ningún turno entre ellos todavía."
                        : " y " + directos + (directos == 1 ? " turno" : " turnos") + " entre ellos ("
                                + historial.h + "-" + historial.ab + ").")
                + (turnosB + bateadoresP < 20 ? " Con tan pocos datos pesa más el promedio de la liga." : "");

        List<TurnoHistorialDto> turnos = directas.stream()
                .map(f -> new TurnoHistorialDto(f.partidoId(), f.fecha(), f.entrada(), f.mitad(), f.resultado(),
                        JuegoService.etiqueta(f.resultado()), f.carreras()))
                .toList();
        return new EnfrentamientoDto(mapeador.res(bateador), mapeador.res(lanzador),
                historial == null ? null : dto(historial), turnos,
                new PronosticoDto(tres(r.hit()), tres(r.embasarse()), tres(r.ponche()), turnosB, bateadoresP,
                        directos, resumen));
    }

    // ------------------------------------------------------------------ cálculo

    Calculo calcular(Long temporadaId) {
        List<FilaMovimiento> movimientos = new ArrayList<>();
        for (Object[] m : jugadas.movimientosEstadisticas(temporadaId)) {
            movimientos.add(new FilaMovimiento(num(m[0]), num(m[1]), (int) num(m[2]),
                    m[3] == null ? null : (int) num(m[3])));
        }
        Map<Long, Long> lanzamientos = new HashMap<>();
        for (Object[] l : jugadas.lanzamientosPorLanzador(temporadaId)) {
            lanzamientos.put(num(l[0]), num(l[1]));
        }
        return CalculadoraEstadisticas.calcular(filas(temporadaId), movimientos, lanzamientos);
    }

    private List<FilaJugada> filas(Long temporadaId) {
        List<FilaJugada> filas = new ArrayList<>();
        for (Object[] f : jugadas.filasEstadisticas(temporadaId)) {
            filas.add(new FilaJugada(num(f[0]), num(f[1]), (int) num(f[2]), (int) num(f[3]), (Mitad) f[4],
                    num(f[5]), num(f[6]), (Resultado) f[7], (int) num(f[8]), (int) num(f[9]), num(f[10]),
                    num(f[11]), (LocalDateTime) f[12], (Partido.Estado) f[13], (int) num(f[14]),
                    (int) num(f[15])));
        }
        return filas;
    }

    private static long num(Object o) {
        return ((Number) o).longValue();
    }

    private static void sumarA(Bateo total, Bateo x) {
        total.pa += x.pa;
        total.ab += x.ab;
        total.h += x.h;
        total.bb += x.bb;
        total.hbp += x.hbp;
        total.sf += x.sf;
        total.so += x.so;
    }

    private BloqueDto bloque(Calculo c, Long jugadorId) {
        Bateo b = c.bateoDe(jugadorId);
        Pitcheo p = c.pitcheoDe(jugadorId);
        boolean conBateo = b != null && b.pa > 0;
        boolean conPitcheo = p != null && (p.outs > 0 || p.bateadores > 0);
        return new BloqueDto(conBateo ? dto(b) : null, conPitcheo ? dto(p) : null);
    }

    static BateoDto dto(Bateo b) {
        return new BateoDto(b.juegos(), b.pa, b.ab, b.h, b.dobles, b.triples, b.hr, b.carreras, b.rbi, b.bb, b.hbp,
                b.so, b.sb, b.cs, b.sf, b.sh, tres(b.avg()), tres(b.obp()), tres(b.slg()), tres(b.ops()));
    }

    static PitcheoDto dto(Pitcheo p) {
        return new PitcheoDto(p.juegos(), p.iniciados.size(), p.ganados, p.perdidos, p.outs, p.ip(), p.bateadores,
                p.h, p.carreras, p.limpias, p.bb, p.hbp, p.so, p.hr, p.lanzamientos, dos(p.era()), dos(p.whip()),
                dos(p.k9()), dos(p.bb9()), tres(p.promedioContra()));
    }

    private static BigDecimal tres(double v) {
        return BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP);
    }

    private static BigDecimal dos(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Long, Jugador> jugadores(Set<Long> ids) {
        Map<Long, Jugador> m = new HashMap<>();
        jugadores.findAllById(ids).forEach(j -> m.put(j.getId(), j));
        return m;
    }

    private Map<Long, EquipoResumen> equipos(Long temporadaId) {
        Map<Long, EquipoResumen> m = new HashMap<>();
        for (Plantilla pl : plantillas.findByTemporadaId(temporadaId)) {
            if (pl.getEquipo() != null) {
                m.put(pl.getJugador().getId(), mapeador.res(pl.getEquipo()));
            }
        }
        return m;
    }

    private Jugador jugador(Long id) {
        return jugadores.findById(id).orElseThrow(() -> ApiException.noEncontrado("Jugador", String.valueOf(id)));
    }

    private void validarTemporada(Long id) {
        if (!temporadas.existsById(id)) {
            throw ApiException.noEncontrado("Temporada", String.valueOf(id));
        }
    }
}
