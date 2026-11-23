package com.ligabeisbolcartagena.main.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartidoRepository extends JpaRepository<Partido, Long> {

    List<Partido> findByTemporadaIdOrderByFechaHoraAsc(Long temporadaId);

    List<Partido> findByTemporadaIdAndEstado(Long temporadaId, Partido.Estado estado);
}
