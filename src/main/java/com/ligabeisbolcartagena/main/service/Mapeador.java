package com.ligabeisbolcartagena.main.service;

import com.ligabeisbolcartagena.main.api.dto.ComunDtos.EquipoResumen;
import com.ligabeisbolcartagena.main.api.dto.ComunDtos.JugadorRef;
import com.ligabeisbolcartagena.main.api.dto.ComunDtos.JugadorResumen;
import com.ligabeisbolcartagena.main.api.dto.EquipoDtos.EquipoDto;
import com.ligabeisbolcartagena.main.api.dto.EstadioDtos.EstadioDto;
import com.ligabeisbolcartagena.main.api.dto.JugadorDtos.JugadorDto;
import com.ligabeisbolcartagena.main.api.dto.JugadorDtos.PlantillaDto;
import com.ligabeisbolcartagena.main.api.dto.TemporadaDtos.TemporadaDto;
import com.ligabeisbolcartagena.main.dominio.Equipo;
import com.ligabeisbolcartagena.main.dominio.Estadio;
import com.ligabeisbolcartagena.main.dominio.Jugador;
import com.ligabeisbolcartagena.main.dominio.Plantilla;
import com.ligabeisbolcartagena.main.dominio.Temporada;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

/** Convierte entidades en DTO. Null-safe: un campo nulo produce null. */
@Component
public class Mapeador {

    private final ZoneId zona;

    public ZoneId zona() {
        return zona;
    }

    public Mapeador(@Value("${lbc.zona-horaria:America/Bogota}") String zona) {
        this.zona = ZoneId.of(zona);
    }

    public TemporadaDto dto(Temporada t) {
        return new TemporadaDto(t.getId(), t.getNombre(), t.getAnio(), t.getEstado(), t.getFechaInicio(),
                t.getFechaFin());
    }

    public EquipoDto dto(Equipo e) {
        return new EquipoDto(e.getId(), e.getNombre(), e.getAbreviatura(), e.getCiudad(), e.getColor(),
                e.getEscudoUrl());
    }

    public EquipoResumen res(Equipo e) {
        if (e == null) {
            return null;
        }
        return new EquipoResumen(e.getId(), e.getNombre(), e.getAbreviatura(), e.getColor(), e.getEscudoUrl());
    }

    public JugadorDto dto(Jugador j) {
        Integer edad = j.getFechaNacimiento() == null ? null
                : Period.between(j.getFechaNacimiento(), LocalDate.now(zona)).getYears();
        return new JugadorDto(j.getId(), j.getNombres(), j.getApellidos(), j.getFechaNacimiento(), edad,
                j.getBatea(), j.getLanza(), j.getFotoUrl());
    }

    public JugadorResumen res(Jugador j) {
        if (j == null) {
            return null;
        }
        return new JugadorResumen(j.getId(), j.getNombres(), j.getApellidos(), j.getBatea(), j.getLanza(),
                j.getFotoUrl());
    }

    public JugadorRef ref(Jugador j) {
        if (j == null) {
            return null;
        }
        return new JugadorRef(j.getId(), j.getNombres() + " " + j.getApellidos());
    }

    public EstadioDto dto(Estadio s) {
        return new EstadioDto(s.getId(), s.getNombre(), s.getUbicacion());
    }

    public PlantillaDto dto(Plantilla p) {
        return new PlantillaDto(p.getId(), res(p.getJugador()), res(p.getEquipo()), p.getNumero(),
                p.getPosicion());
    }
}
