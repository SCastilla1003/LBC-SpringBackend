package com.ligabeisbolcartagena.main.dominio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JugadaRepository extends JpaRepository<Jugada, Long> {

    /** Filas (entrada, mitad, carreras) para el marcador por entrada. */
    @Query("select j.entrada, j.mitad, sum(j.carreras) from Jugada j "
            + "where j.partido.id = :partidoId group by j.entrada, j.mitad")
    List<Object[]> carrerasPorEntrada(@Param("partidoId") Long partidoId);
}
