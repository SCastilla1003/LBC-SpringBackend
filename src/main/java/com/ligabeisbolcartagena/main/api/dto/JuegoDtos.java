package com.ligabeisbolcartagena.main.api.dto;

import com.ligabeisbolcartagena.main.api.dto.ComunDtos.EquipoResumen;
import com.ligabeisbolcartagena.main.api.dto.ComunDtos.JugadorResumen;
import com.ligabeisbolcartagena.main.api.dto.PartidoDtos.PartidoDetalleDto;
import com.ligabeisbolcartagena.main.dominio.Jugada;
import com.ligabeisbolcartagena.main.dominio.Lanzamiento;
import com.ligabeisbolcartagena.main.dominio.Mitad;
import com.ligabeisbolcartagena.main.dominio.Partido;
import com.ligabeisbolcartagena.main.dominio.Sustitucion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class JuegoDtos {

    private JuegoDtos() {
    }

    /** Todo lo que necesita la pantalla del anotador. */
    public record EstadoJuegoDto(PartidoDetalleDto partido, EquipoJuegoDto local, EquipoJuegoDto visita,
                                 boolean alineacionCompleta, List<String> faltantes, boolean puedeDeshacer,
                                 List<AccionDto> jugadas) {
    }

    /** Orden de bateo actual, lanzador actual (con sus lanzamientos) y quién puede entrar de cambio. */
    public record EquipoJuegoDto(EquipoResumen equipo, List<TurnoDto> orden, int proximo, JugadorResumen lanzador,
                                 long lanzamientos, List<JugadorResumen> disponibles) {
    }

    public record TurnoDto(int orden, String posicion, JugadorResumen jugador) {
    }

    public record AccionDto(Long id, String tipo, String descripcion, int entrada, Mitad mitad,
                            LocalDateTime hora) {
    }

    /** commandId: UUID que genera la app para que un reintento no se aplique dos veces. */
    public record ComandoRequest(@NotBlank(message = "Falta el commandId") @Size(max = 36) String commandId) {
    }

    /** motivo: retrasado, pospuesto o cancelado (null = suspensión sin motivo). */
    public record SuspensionRequest(
            @NotBlank(message = "Falta el commandId") @Size(max = 36) String commandId,
            Partido.Motivo motivo) {
    }

    /** El administrador decide: continuar el partido o dejarlo así. */
    public record DecisionRequest(
            @NotBlank(message = "Falta el commandId") @Size(max = 36) String commandId,
            @NotNull(message = "Indica si el partido continúa") Boolean continuar) {
    }

    public record LanzamientoRequest(
            @NotBlank(message = "Falta el commandId") @Size(max = 36) String commandId,
            @NotNull(message = "El tipo de lanzamiento es obligatorio") Lanzamiento.Tipo tipo) {
    }

    /** hasta: 1..3 = base, 4 = anota, null = out. desde: 0 = bateador, 1..3 = base. */
    public record MovimientoRequest(
            @Min(0) @Max(3) int desde,
            @Min(1) @Max(4) Integer hasta) {
    }

    /** movimientos null = avance típico de la jugada. */
    public record JugadaRequest(
            @NotBlank(message = "Falta el commandId") @Size(max = 36) String commandId,
            @NotNull(message = "El resultado es obligatorio") Jugada.Resultado resultado,
            @Valid List<MovimientoRequest> movimientos,
            @Min(0) @Max(4) Integer errores) {
    }

    /** local: si el cambio es del equipo local. Con saleId == entraId solo cambia la posición. */
    public record CambioRequest(
            @NotBlank(message = "Falta el commandId") @Size(max = 36) String commandId,
            @NotNull(message = "El tipo de cambio es obligatorio") Sustitucion.Tipo tipo,
            @NotNull(message = "Indica el equipo") Boolean local,
            @NotNull(message = "Indica quién sale") Long saleId,
            @NotNull(message = "Indica quién entra") Long entraId,
            String posicion) {
    }
}
