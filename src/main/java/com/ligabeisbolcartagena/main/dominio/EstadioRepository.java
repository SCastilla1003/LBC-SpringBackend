package com.ligabeisbolcartagena.main.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstadioRepository extends JpaRepository<Estadio, Long> {

    List<Estadio> findAllByOrderByNombreAsc();

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);
}
