package com.ligabeisbolcartagena.main.dominio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JugadaRepository extends JpaRepository<Jugada, Long> {

    /** Filas (entrada, mitad, carreras) para el marcador por entrada. */
    @Query("select j.entrada, j.mitad, sum(j.carreras) from Jugada j "
            + "where j.partido.id = :partidoId group by j.entrada, j.mitad")
    List<Object[]> carrerasPorEntrada(@Param("partidoId") Long partidoId);

    Optional<Jugada> findTopByPartidoIdOrderBySecuenciaDesc(Long partidoId);

    /** Jugadas para estadísticas (temporadaId nulo = todas). Orden de columnas: ver EstadisticasService. */
    @Query("select j.id, p.id, j.secuencia, j.entrada, j.mitad, j.bateador.id, j.lanzador.id, j.resultado, "
            + "j.carreras, j.outsRegistrados, p.equipoLocal.id, p.equipoVisita.id, p.fechaHora, p.estado, "
            + "p.carrerasLocal, p.carrerasVisita from Jugada j join j.partido p "
            + "where (:temporadaId is null or p.temporada.id = :temporadaId)")
    List<Object[]> filasEstadisticas(@Param("temporadaId") Long temporadaId);

    @Query("select m.jugada.id, m.jugador.id, m.baseDesde, m.baseHasta from MovimientoJugada m "
            + "join m.jugada j join j.partido p where (:temporadaId is null or p.temporada.id = :temporadaId)")
    List<Object[]> movimientosEstadisticas(@Param("temporadaId") Long temporadaId);

    @Query("select l.lanzador.id, count(l) from Lanzamiento l join l.partido p "
            + "where (:temporadaId is null or p.temporada.id = :temporadaId) group by l.lanzador.id")
    List<Object[]> lanzamientosPorLanzador(@Param("temporadaId") Long temporadaId);
}
