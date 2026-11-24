package com.ligabeisbolcartagena.main.api.dto;

import com.ligabeisbolcartagena.main.api.dto.ComunDtos.EquipoResumen;
import com.ligabeisbolcartagena.main.api.dto.ComunDtos.JugadorResumen;
import com.ligabeisbolcartagena.main.dominio.Jugador;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class JugadorDtos {

    private JugadorDtos() {
    }

    public record JugadorDto(Long id, String nombres, String apellidos, LocalDate fechaNacimiento, Integer edad,
                             Jugador.LadoBateo batea, Jugador.LadoLanzamiento lanza, String fotoUrl,
                             boolean esLanzador) {
    }

    public record JugadorRequest(
            @NotBlank(message = "Los nombres son obligatorios")
            @Size(max = 100, message = "Los nombres no pueden tener más de 100 caracteres")
            String nombres,
            @NotBlank(message = "Los apellidos son obligatorios")
            @Size(max = 100, message = "Los apellidos no pueden tener más de 100 caracteres")
            String apellidos,
            LocalDate fechaNacimiento,
            Jugador.LadoBateo batea,
            Jugador.LadoLanzamiento lanza,
            /** Es pitcher. Nulo: al crear, se toma como pitcher si trae mano de lanzar; al editar, no cambia. */
            Boolean esLanzador,
            @Size(max = 500, message = "La URL de la foto es demasiado larga")
            String fotoUrl) {
    }

    /** Entrada de la plantilla de una temporada. equipo nulo = agente libre. */
    public record PlantillaDto(Long id, JugadorResumen jugador, EquipoResumen equipo, Integer numero,
                               String posicion) {
    }

    public record PlantillaRequest(
            @NotNull(message = "El jugador es obligatorio")
            Long jugadorId,
            Long equipoId,
            @Min(value = 0, message = "El número no puede ser negativo")
            @Max(value = 99, message = "El número debe ser 99 o menor")
            Integer numero,
            @Pattern(regexp = ComunDtos.POSICION_REGEX, message = "Posición no válida")
            String posicion) {
    }

    public record PlantillaActualizacion(
            Long equipoId,
            @Min(value = 0, message = "El número no puede ser negativo")
            @Max(value = 99, message = "El número debe ser 99 o menor")
            Integer numero,
            @Pattern(regexp = ComunDtos.POSICION_REGEX, message = "Posición no válida")
            String posicion) {
    }
}
