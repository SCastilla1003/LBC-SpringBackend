package com.ligabeisbolcartagena.main.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PosicionAlineacionRepository extends JpaRepository<PosicionAlineacion, Long> {

    List<PosicionAlineacion> findByPartidoIdOrderByIdAsc(Long partidoId);

    void deleteByPartidoId(Long partidoId);

    long countByJugadorIdAndPartidoTemporadaId(Long jugadorId, Long temporadaId);
}
