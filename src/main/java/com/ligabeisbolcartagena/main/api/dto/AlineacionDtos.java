package com.ligabeisbolcartagena.main.api.dto;

import com.ligabeisbolcartagena.main.api.dto.ComunDtos.EquipoResumen;
import com.ligabeisbolcartagena.main.api.dto.ComunDtos.JugadorResumen;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public final class AlineacionDtos {

    private AlineacionDtos() {
    }

    public record AlineacionDto(boolean publicada, EquipoAlineacionDto local, EquipoAlineacionDto visita) {
    }

    public record EquipoAlineacionDto(EquipoResumen equipo, JugadorResumen lanzadorInicial,
                                      List<BateadorDto> bateo, List<JugadorResumen> banca) {
    }

    public record BateadorDto(int orden, String posicion, JugadorResumen jugador) {
    }

    public record PitcheoDto(boolean publicada, EquipoPitcheoDto local, EquipoPitcheoDto visita) {
    }

    public record EquipoPitcheoDto(EquipoResumen equipo, JugadorResumen lanzadorInicial,
                                   List<JugadorResumen> relevistas) {
    }

    public record AlineacionRequest(
            boolean publicar,
            @NotNull(message = "La alineación del local es obligatoria") @Valid EquipoAlineacionRequest local,
            @NotNull(message = "La alineación del visitante es obligatoria") @Valid EquipoAlineacionRequest visita) {
    }

    public record EquipoAlineacionRequest(
            Long lanzadorInicial,
            @Valid List<BateoRequest> bateo,
            List<Long> banca) {
    }

    public record BateoRequest(
            @NotNull(message = "El orden de bateo es obligatorio")
            @Min(value = 1, message = "El orden de bateo va de 1 a 9")
            @Max(value = 9, message = "El orden de bateo va de 1 a 9")
            Integer orden,
            @NotNull(message = "El jugador es obligatorio")
            Long jugadorId,
            @NotNull(message = "La posición es obligatoria")
            @Pattern(regexp = ComunDtos.POSICION_REGEX, message = "Posición no válida")
            String posicion) {
    }
}
