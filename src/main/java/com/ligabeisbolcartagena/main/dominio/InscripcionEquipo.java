package com.ligabeisbolcartagena.main.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Equipo inscrito en una temporada (puede no tener jugadores todavía). */
@Entity
@Table(name = "season_team")
public class InscripcionEquipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id")
    private Temporada temporada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id")
    private Equipo equipo;

    public InscripcionEquipo() {
    }

    public InscripcionEquipo(Temporada temporada, Equipo equipo) {
        this.temporada = temporada;
        this.equipo = equipo;
    }

    public Long getId() { return id; }
    public Temporada getTemporada() { return temporada; }
    public Equipo getEquipo() { return equipo; }
}
