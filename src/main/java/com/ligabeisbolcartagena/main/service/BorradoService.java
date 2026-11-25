package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.AuditoriaDtos.ImpactoDto;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.dominio.Jugador;
import com.ligabeisbolcartagena.main.dominio.JugadorRepository;
import com.ligabeisbolcartagena.main.dominio.Partido;
import com.ligabeisbolcartagena.main.dominio.PartidoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * "Eliminar por completo": borra un jugador o un partido junto con todo lo que depende de él
 * (inscripciones, alineaciones, jugadas, lanzamientos, cambios...). No se puede deshacer; por
 * eso primero se puede consultar el impacto.
 */
@Service
public class BorradoService {

    @PersistenceContext
    private EntityManager em;

    private final JugadorRepository jugadores;
    private final PartidoRepository partidos;
    private final PartidoService partidoService;
    private final FotoService fotos;
    private final AuditoriaService auditoria;
    private final Mapeador mapeador;

    public BorradoService(JugadorRepository jugadores, PartidoRepository partidos, PartidoService partidoService,
                          FotoService fotos, AuditoriaService auditoria, Mapeador mapeador) {
        this.jugadores = jugadores;
        this.partidos = partidos;
        this.partidoService = partidoService;
        this.fotos = fotos;
        this.auditoria = auditoria;
        this.mapeador = mapeador;
    }

    // ------------------------------------------------------------------ jugador

    /** Jugadas donde batea, lanza o defiende el jugador (y sus lanzamientos y movimientos). */
    private static final String SUS_JUGADAS =
            "select id from game_play where bateador_id = :id or lanzador_id = :id or jugador_defensa_id = :id";

    @Transactional(readOnly = true)
    public ImpactoDto impactoJugador(Long id) {
        Jugador j = buscarJugador(id);
        return new ImpactoDto(
                AuditoriaService.nombre(j.getNombres(), j.getApellidos()),
                contar("select count(*) from roster_entry where player_id = :id", id),
                contar("select count(*) from lineup_spot where player_id = :id", id),
                contar("select count(distinct game_id) from lineup_spot where player_id = :id", id),
                contar("select count(*) from game_play where bateador_id = :id or lanzador_id = :id "
                        + "or jugador_defensa_id = :id", id),
                contar("select count(*) from game_pitch where bateador_id = :id or lanzador_id = :id", id),
                contar("select count(*) from game_substitution where jugador_sale_id = :id or jugador_entra_id = :id", id),
                enJuego(id));
    }

    @Transactional
    public void eliminarJugador(Long id) {
        Jugador j = buscarJugador(id);
        if (enJuego(id)) {
            throw ApiException.conflicto(AuditoriaService.nombre(j.getNombres(), j.getApellidos())
                    + " está en un partido en juego o suspendido: termínalo antes de borrarlo");
        }
        var antes = mapeador.dto(j);
        var impacto = impactoJugador(id);
        Map<String, Object> p = Map.of("id", id);
        // Lo que crearon sus jugadas en el jugada a jugada y luego las jugadas mismas.
        ejecutar("delete from game_action where play_id in (" + SUS_JUGADAS + ") "
                + "or pitch_id in (select id from game_pitch where bateador_id = :id or lanzador_id = :id) "
                + "or substitution_id in (select id from game_substitution where jugador_sale_id = :id "
                + "or jugador_entra_id = :id)", p);
        ejecutar("delete from game_pitch where bateador_id = :id or lanzador_id = :id "
                + "or play_id in (" + SUS_JUGADAS + ")", p);
        ejecutar("delete from game_play_movement where jugador_id = :id or play_id in (" + SUS_JUGADAS + ")", p);
        ejecutar("delete from game_play where bateador_id = :id or lanzador_id = :id or jugador_defensa_id = :id", p);
        ejecutar("delete from game_substitution where jugador_sale_id = :id or jugador_entra_id = :id", p);
        ejecutar("delete from lineup_spot where player_id = :id", p);
        ejecutar("update game set bateador_id = null where bateador_id = :id", p);
        ejecutar("update game set lanzador_id = null where lanzador_id = :id", p);
        ejecutar("update game set corredor_1b_id = null where corredor_1b_id = :id", p);
        ejecutar("update game set corredor_2b_id = null where corredor_2b_id = :id", p);
        ejecutar("update game set corredor_3b_id = null where corredor_3b_id = :id", p);
        ejecutar("delete from player_stat where player_id = :id", p);
        ejecutar("delete from roster_entry where player_id = :id", p);
        ejecutar("delete from favorite where target_type = 'PLAYER' and target_id = :id", p);
        em.flush();
        em.clear();
        ejecutar("delete from player where id = :id", p);
        fotos.borrarArchivoJugador(id);
        auditoria.registrar("ELIMINAR_COMPLETO", "Jugador", id,
                "Eliminó por completo al jugador " + impacto.nombre() + " (" + impacto.inscripciones()
                        + " inscripciones, " + impacto.alineaciones() + " alineaciones, " + impacto.jugadas()
                        + " jugadas)", antes, null);
    }

    // ------------------------------------------------------------------ partido

    @Transactional(readOnly = true)
    public ImpactoDto impactoPartido(Long id) {
        Partido p = buscarPartido(id);
        return new ImpactoDto(
                nombrePartido(p),
                0,
                contar("select count(*) from lineup_spot where game_id = :id", id),
                1,
                contar("select count(*) from game_play where game_id = :id", id),
                contar("select count(*) from game_pitch where game_id = :id", id),
                contar("select count(*) from game_substitution where game_id = :id", id),
                p.getEstado() == Partido.Estado.LIVE);
    }

    /** Borra el partido en cualquier estado; alineación, jugadas y demás se van con él. */
    @Transactional
    public void eliminarPartido(Long id) {
        Partido p = buscarPartido(id);
        var antes = partidoService.detalle(p);
        var impacto = impactoPartido(id);
        Map<String, Object> params = Map.of("id", id);
        ejecutar("delete from favorite where target_type = 'GAME' and target_id = :id", params);
        em.flush();
        em.clear();
        // Las tablas del partido (alineación, jugadas, lanzamientos, cambios, acciones) se borran en cascada.
        ejecutar("delete from game where id = :id", params);
        auditoria.registrar("ELIMINAR_COMPLETO", "Partido", id,
                "Eliminó por completo el partido " + impacto.nombre() + " (" + impacto.jugadas() + " jugadas, "
                        + impacto.lanzamientos() + " lanzamientos)", antes, null);
    }

    static String nombrePartido(Partido p) {
        return p.getEquipoVisita().getAbreviatura() + " @ " + p.getEquipoLocal().getAbreviatura() + " del "
                + p.getFechaHora().toLocalDate();
    }

    // ------------------------------------------------------------------ ayudas

    private boolean enJuego(Long jugador) {
        return contar("select count(*) from lineup_spot l join game g on g.id = l.game_id "
                + "where l.player_id = :id and g.estado in ('LIVE', 'SUSPENDED')", jugador) > 0
                || contar("select count(*) from game_substitution s join game g on g.id = s.game_id "
                + "where s.jugador_entra_id = :id and g.estado in ('LIVE', 'SUSPENDED')", jugador) > 0;
    }

    private long contar(String sql, Long id) {
        return ((Number) em.createNativeQuery(sql).setParameter("id", id).getSingleResult()).longValue();
    }

    private void ejecutar(String sql, Map<String, Object> params) {
        var q = em.createNativeQuery(sql);
        params.forEach(q::setParameter);
        q.executeUpdate();
    }

    private Jugador buscarJugador(Long id) {
        return jugadores.findById(id).orElseThrow(() -> ApiException.noEncontrado("Jugador", String.valueOf(id)));
    }

    private Partido buscarPartido(Long id) {
        return partidos.findById(id).orElseThrow(() -> ApiException.noEncontrado("Partido", String.valueOf(id)));
    }
}
