package com.ligabeisbolcartagena.main.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TemporadaRepository extends JpaRepository<Temporada, Long> {

    List<Temporada> findAllByOrderByAnioDescIdDesc();

    Optional<Temporada> findFirstByEstadoOrderByAnioDescIdDesc(Temporada.Estado estado);

    boolean existsByNombreAndAnio(String nombre, Integer anio);

    boolean existsByNombreAndAnioAndIdNot(String nombre, Integer anio, Long id);
}
