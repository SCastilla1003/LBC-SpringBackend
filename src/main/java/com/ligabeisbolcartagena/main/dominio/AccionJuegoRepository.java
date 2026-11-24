package com.ligabeisbolcartagena.main.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccionJuegoRepository extends JpaRepository<AccionJuego, Long> {

    Optional<AccionJuego> findTopByPartidoIdOrderByIdDesc(Long partidoId);

    boolean existsByPartidoIdAndCommandId(Long partidoId, String commandId);

    List<AccionJuego> findTop30ByPartidoIdOrderByIdDesc(Long partidoId);
}
