package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.AlineacionDtos.AlineacionDto;
import com.ligabeisbolcartagena.main.api.dto.AlineacionDtos.AlineacionRequest;
import com.ligabeisbolcartagena.main.api.dto.AlineacionDtos.BateadorDto;
import com.ligabeisbolcartagena.main.api.dto.AlineacionDtos.BateoRequest;
import com.ligabeisbolcartagena.main.api.dto.AlineacionDtos.EquipoAlineacionDto;
import com.ligabeisbolcartagena.main.api.dto.AlineacionDtos.EquipoAlineacionRequest;
import com.ligabeisbolcartagena.main.api.dto.AlineacionDtos.EquipoPitcheoDto;
import com.ligabeisbolcartagena.main.api.dto.AlineacionDtos.PitcheoDto;
import com.ligabeisbolcartagena.main.api.dto.ComunDtos.JugadorResumen;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.dominio.Equipo;
import com.ligabeisbolcartagena.main.dominio.Jugador;
import com.ligabeisbolcartagena.main.dominio.Partido;
import com.ligabeisbolcartagena.main.dominio.PartidoRepository;
import com.ligabeisbolcartagena.main.dominio.Plantilla;
import com.ligabeisbolcartagena.main.dominio.PlantillaRepository;
import com.ligabeisbolcartagena.main.dominio.PosicionAlineacion;
import com.ligabeisbolcartagena.main.dominio.PosicionAlineacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Alineaciones de cada partido. El lanzador inicial normalmente no batea (la liga usa DH); si batea,
 * va en el orden de bateo con posición P, o como DH (regla Ohtani). Un jugador no puede aparecer dos
 * veces en la alineación ni dos jugadores en la misma posición.
 */
@Service
public class AlineacionService {

    static final String POSICION_LANZADOR = "P";
    static final String POSICION_DH = "DH";

    private final PartidoRepository partidos;
    private final PlantillaRepository plantillas;
    private final PosicionAlineacionRepository posiciones;
    private final Mapeador mapeador;

    public AlineacionService(PartidoRepository partidos, PlantillaRepository plantillas,
                             PosicionAlineacionRepository posiciones, Mapeador mapeador) {
        this.partidos = partidos;
        this.plantillas = plantillas;
        this.posiciones = posiciones;
        this.mapeador = mapeador;
    }

    @Transactional(readOnly = true)
    public AlineacionDto obtener(Long partidoId, boolean privilegiado) {
        Partido p = buscar(partidoId);
        boolean publicada = publicada(p);
        if (!publicada && !privilegiado) {
            return new AlineacionDto(false, null, null);
        }
        List<PosicionAlineacion> filas = posiciones.findByPartidoIdOrderByIdAsc(partidoId);
        return new AlineacionDto(publicada, equipoAlineacion(p.getEquipoLocal(), filas),
                equipoAlineacion(p.getEquipoVisita(), filas));
    }

    @Transactional(readOnly = true)
    public PitcheoDto pitcheo(Long partidoId, boolean privilegiado) {
        Partido p = buscar(partidoId);
        boolean publicada = publicada(p);
        if (!publicada && !privilegiado) {
            return new PitcheoDto(false, null, null);
        }
        List<PosicionAlineacion> filas = posiciones.findByPartidoIdOrderByIdAsc(partidoId);
        return new PitcheoDto(publicada, equipoPitcheo(p.getEquipoLocal(), filas),
                equipoPitcheo(p.getEquipoVisita(), filas));
    }

    @Transactional
    public AlineacionDto guardar(Long partidoId, AlineacionRequest req) {
        Partido p = buscar(partidoId);
        if (p.getEstado() != Partido.Estado.SCHEDULED && p.getEstado() != Partido.Estado.LINEUP_PUBLISHED) {
            throw ApiException.conflicto("La alineación solo se puede cambiar antes de que empiece el partido");
        }
        Long temporadaId = p.getTemporada().getId();
        List<Plantilla> deLocal = plantillas.findByTemporadaIdAndEquipoId(temporadaId, p.getEquipoLocal().getId());
        List<Plantilla> deVisita = plantillas.findByTemporadaIdAndEquipoId(temporadaId, p.getEquipoVisita().getId());

        Map<String, Object> errores = new LinkedHashMap<>();
        List<PosicionAlineacion> filas = new ArrayList<>();
        construir(p, p.getEquipoLocal(), req.local(), deLocal, req.publicar(), "local", filas, errores);
        construir(p, p.getEquipoVisita(), req.visita(), deVisita, req.publicar(), "visita", filas, errores);
        if (!errores.isEmpty()) {
            throw ApiException.invalida("La alineación tiene datos no válidos", errores);
        }

        posiciones.deleteByPartidoId(partidoId);
        posiciones.flush();
        posiciones.saveAll(filas);
        if (req.publicar() && p.getEstado() == Partido.Estado.SCHEDULED) {
            p.setEstado(Partido.Estado.LINEUP_PUBLISHED);
            partidos.save(p);
        }
        return obtener(partidoId, true);
    }

    private void construir(Partido p, Equipo equipo, EquipoAlineacionRequest eq, List<Plantilla> plantilla,
                           boolean publicar, String lado, List<PosicionAlineacion> filas,
                           Map<String, Object> errores) {
        Map<Long, Jugador> enPlantilla = new HashMap<>();
        for (Plantilla pl : plantilla) {
            enPlantilla.put(pl.getJugador().getId(), pl.getJugador());
        }
        List<BateoRequest> bateo = eq.bateo() == null ? List.of() : eq.bateo();
        List<Long> banca = eq.banca() == null ? List.of() : eq.banca();

        Set<Long> usados = new HashSet<>();
        Set<Integer> ordenes = new HashSet<>();
        Map<String, Integer> posicionesUsadas = new HashMap<>();
        for (int i = 0; i < bateo.size(); i++) {
            BateoRequest b = bateo.get(i);
            String clave = lado + ".bateo[" + i + "]";
            if (!ordenes.add(b.orden())) {
                errores.put(clave, "el orden " + b.orden() + " está repetido");
            }
            Jugador j = enPlantilla.get(b.jugadorId());
            if (j == null) {
                errores.put(clave, "el jugador no está en la plantilla de " + equipo.getNombre() + " en esta temporada");
                continue;
            }
            if (!usados.add(j.getId())) {
                errores.put(clave, "el jugador está repetido en la alineación");
                continue;
            }
            if (POSICION_LANZADOR.equals(b.posicion()) && !b.jugadorId().equals(eq.lanzadorInicial())) {
                errores.put(clave, "la posición P solo puede ser del lanzador inicial");
            }
            Integer otro = b.posicion() == null ? null : posicionesUsadas.putIfAbsent(b.posicion(), b.orden());
            if (otro != null) {
                errores.put(clave, "la posición " + b.posicion() + " ya la tiene el turno " + otro);
            }
            filas.add(fila(p, equipo, j, b.orden(), b.posicion(), true));
        }
        if (publicar && ordenes.size() != 9) {
            errores.put(lado + ".bateo", "para publicar debe haber 9 bateadores con orden del 1 al 9");
        }

        if (eq.lanzadorInicial() == null) {
            if (publicar) {
                errores.put(lado + ".lanzadorInicial", "es obligatorio para publicar");
            }
        } else {
            Jugador lanzador = enPlantilla.get(eq.lanzadorInicial());
            String clave = lado + ".lanzadorInicial";
            if (lanzador == null) {
                errores.put(clave, "no está en la plantilla de " + equipo.getNombre() + " en esta temporada");
            } else if (!lanzador.isEsLanzador()) {
                errores.put(clave, "el jugador no está marcado como lanzador (pitcher)");
            } else if (usados.contains(lanzador.getId())) {
                // Batea él mismo: como P (sin DH) o como DH (regla Ohtani).
                PosicionAlineacion propia = filas.stream()
                        .filter(f -> f.getEquipo() == equipo && f.getJugador().getId().equals(lanzador.getId()))
                        .findFirst().orElse(null);
                if (propia == null || !(POSICION_LANZADOR.equals(propia.getPosicion())
                        || POSICION_DH.equals(propia.getPosicion()))) {
                    errores.put(clave, "si el lanzador inicial batea, va como P o como DH");
                } else {
                    propia.setLanzadorInicial(true);
                }
            } else {
                usados.add(lanzador.getId());
                PosicionAlineacion f = fila(p, equipo, lanzador, null, POSICION_LANZADOR, true);
                f.setLanzadorInicial(true);
                filas.add(f);
            }
        }

        for (int i = 0; i < banca.size(); i++) {
            String clave = lado + ".banca[" + i + "]";
            Jugador j = enPlantilla.get(banca.get(i));
            if (j == null) {
                errores.put(clave, "el jugador no está en la plantilla de " + equipo.getNombre() + " en esta temporada");
            } else if (!usados.add(j.getId())) {
                errores.put(clave, "el jugador ya está en la alineación");
            } else {
                filas.add(fila(p, equipo, j, null, null, false));
            }
        }
    }

    private static PosicionAlineacion fila(Partido p, Equipo equipo, Jugador jugador, Integer orden, String posicion,
                                           boolean titular) {
        PosicionAlineacion f = new PosicionAlineacion();
        f.setPartido(p);
        f.setEquipo(equipo);
        f.setJugador(jugador);
        f.setOrdenBateo(orden);
        f.setPosicion(posicion);
        f.setTitular(titular);
        return f;
    }

    private EquipoAlineacionDto equipoAlineacion(Equipo equipo, List<PosicionAlineacion> filas) {
        List<BateadorDto> bateo = filas.stream()
                .filter(f -> f.getEquipo().getId().equals(equipo.getId()) && f.getOrdenBateo() != null)
                .sorted(Comparator.comparing(PosicionAlineacion::getOrdenBateo))
                .map(f -> new BateadorDto(f.getOrdenBateo(), f.getPosicion(), mapeador.res(f.getJugador())))
                .toList();
        JugadorResumen lanzador = lanzadorInicial(equipo, filas);
        List<JugadorResumen> banca = filas.stream()
                .filter(f -> f.getEquipo().getId().equals(equipo.getId()) && !f.isTitular())
                .map(f -> mapeador.res(f.getJugador()))
                .toList();
        return new EquipoAlineacionDto(mapeador.res(equipo), lanzador, bateo, banca);
    }

    private EquipoPitcheoDto equipoPitcheo(Equipo equipo, List<PosicionAlineacion> filas) {
        List<JugadorResumen> relevistas = filas.stream()
                .filter(f -> f.getEquipo().getId().equals(equipo.getId()) && !f.isTitular()
                        && f.getJugador().isEsLanzador())
                .map(f -> mapeador.res(f.getJugador()))
                .toList();
        return new EquipoPitcheoDto(mapeador.res(equipo), lanzadorInicial(equipo, filas), relevistas);
    }

    private JugadorResumen lanzadorInicial(Equipo equipo, List<PosicionAlineacion> filas) {
        return filas.stream()
                .filter(f -> f.getEquipo().getId().equals(equipo.getId())
                        && (f.isLanzadorInicial() || POSICION_LANZADOR.equals(f.getPosicion())))
                .findFirst()
                .map(f -> mapeador.res(f.getJugador()))
                .orElse(null);
    }

    private static boolean publicada(Partido p) {
        return p.getEstado() != Partido.Estado.SCHEDULED;
    }

    private Partido buscar(Long id) {
        return partidos.findById(id).orElseThrow(() -> ApiException.noEncontrado("Partido", String.valueOf(id)));
    }
}
