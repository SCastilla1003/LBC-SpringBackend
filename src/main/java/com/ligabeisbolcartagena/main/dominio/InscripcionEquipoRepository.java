package com.ligabeisbolcartagena.main.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InscripcionEquipoRepository extends JpaRepository<InscripcionEquipo, Long> {

    List<InscripcionEquipo> findByTemporadaId(Long temporadaId);

    boolean existsByTemporadaIdAndEquipoId(Long temporadaId, Long equipoId);

    Optional<InscripcionEquipo> findByTemporadaIdAndEquipoId(Long temporadaId, Long equipoId);
}
