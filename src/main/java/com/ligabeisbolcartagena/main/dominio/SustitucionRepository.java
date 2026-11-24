package com.ligabeisbolcartagena.main.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SustitucionRepository extends JpaRepository<Sustitucion, Long> {

    List<Sustitucion> findByPartidoIdOrderByIdAsc(Long partidoId);
}
