package com.ligabeisbolcartagena.main.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlantillaRepository extends JpaRepository<Plantilla, Long> {

    List<Plantilla> findByTemporadaId(Long temporadaId);

    List<Plantilla> findByTemporadaIdAndEquipoId(Long temporadaId, Long equipoId);

    List<Plantilla> findByTemporadaIdAndEquipoIsNull(Long temporadaId);

    boolean existsByTemporadaIdAndJugadorId(Long temporadaId, Long jugadorId);

    Optional<Plantilla> findByIdAndTemporadaId(Long id, Long temporadaId);
}
