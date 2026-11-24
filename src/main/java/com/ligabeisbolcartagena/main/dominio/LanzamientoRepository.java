package com.ligabeisbolcartagena.main.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LanzamientoRepository extends JpaRepository<Lanzamiento, Long> {

    long countByPartidoIdAndLanzadorId(Long partidoId, Long lanzadorId);
}
