package com.ligabeisbolcartagena.main.api.dto;

import com.ligabeisbolcartagena.main.api.dto.ComunDtos.EquipoResumen;
import com.ligabeisbolcartagena.main.api.dto.ComunDtos.JugadorRef;
import com.ligabeisbolcartagena.main.api.dto.EstadioDtos.EstadioDto;
import com.ligabeisbolcartagena.main.dominio.Mitad;
import com.ligabeisbolcartagena.main.dominio.Partido;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class PartidoDtos {

    private PartidoDtos() {
    }

    /** Marcador o pareja local/visita (carreras, hits, errores). */
    public record ParLocalVisita(int local, int visita) {
    }

    public record PartidoResumenDto(Long id, Long temporadaId, LocalDateTime fechaHora, Partido.Estado estado,
                                    EstadioDto estadio, EquipoResumen equipoLocal, EquipoResumen equipoVisita,
                                    ParLocalVisita marcador, Long ganadorId, Partido.Motivo motivo,
                                    Partido.FinalEspecial finalEspecial) {
    }

    public record PartidoDetalleDto(Long id, Long temporadaId, LocalDateTime fechaHora, Partido.Estado estado,
                                    EstadioDto estadio, EquipoResumen equipoLocal, EquipoResumen equipoVisita,
                                    ParLocalVisita marcador, Long ganadorId, int entradasProgramadas,
                                    int entradaActual, Mitad mitad, SituacionDto situacion,
                                    ParLocalVisita hits, ParLocalVisita errores, LineScoreDto lineScore,
                                    Long version, Partido.Motivo motivo, Partido.FinalEspecial finalEspecial) {
    }

    public record SituacionDto(int outs, int bolas, int strikes, JugadorRef bateador, JugadorRef lanzador,
                               CorredoresDto corredores) {
    }

    public record CorredoresDto(JugadorRef primera, JugadorRef segunda, JugadorRef tercera) {
    }

    /** Marcador por entrada. Las entradas sin jugar llevan null. */
    public record LineScoreDto(int entradasProgramadas, boolean entradasExtra,
                               List<EntradaLineScoreDto> entradas) {
    }

    public record EntradaLineScoreDto(int numero, Integer local, Integer visita) {
    }

    public record PosicionDto(EquipoResumen equipo, int juegos, int ganados, int perdidos, BigDecimal pct,
                              int carrerasFavor, int carrerasContra) {
    }

    public record PartidoRequest(
            @NotNull(message = "El equipo local es obligatorio")
            Long equipoLocalId,
            @NotNull(message = "El equipo visitante es obligatorio")
            Long equipoVisitaId,
            @NotNull(message = "La fecha y hora son obligatorias")
            LocalDateTime fechaHora,
            Long estadioId,
            @Min(value = 1, message = "Las entradas deben ser entre 1 y 15")
            @Max(value = 15, message = "Las entradas deben ser entre 1 y 15")
            Integer entradasProgramadas) {
    }

    /** Todos los campos son opcionales: solo se cambia lo que venga. */
    public record PartidoActualizacion(
            LocalDateTime fechaHora,
            Long estadioId,
            @Min(value = 1, message = "Las entradas deben ser entre 1 y 15")
            @Max(value = 15, message = "Las entradas deben ser entre 1 y 15")
            Integer entradasProgramadas) {
    }
}
