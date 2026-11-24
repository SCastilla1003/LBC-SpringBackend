package com.ligabeisbolcartagena.main.api.dto;

import com.ligabeisbolcartagena.main.api.dto.ComunDtos.EquipoResumen;
import com.ligabeisbolcartagena.main.api.dto.ComunDtos.JugadorResumen;
import com.ligabeisbolcartagena.main.dominio.Jugada;
import com.ligabeisbolcartagena.main.dominio.Mitad;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class EstadisticasDtos {

    private EstadisticasDtos() {
    }

    /** avg, obp, slg y ops con 3 decimales (0.256). */
    public record BateoDto(int juegos, int pa, int ab, int h, int dobles, int triples, int hr, int carreras, int rbi,
                           int bb, int hbp, int so, int sb, int cs, int sf, int sh, BigDecimal avg, BigDecimal obp,
                           BigDecimal slg, BigDecimal ops) {
    }

    /** ip en formato de béisbol ("5.2" = 5 entradas y 2 outs); outs para ordenar y sumar. */
    public record PitcheoDto(int juegos, int iniciados, int ganados, int perdidos, int outs, String ip, int bateadores,
                             int hits, int carreras, int limpias, int bb, int hbp, int so, int hr, int lanzamientos,
                             BigDecimal era, BigDecimal whip, BigDecimal k9, BigDecimal bb9,
                             BigDecimal promedioContra) {
    }

    /** null en bateo o pitcheo = no tiene datos de eso. */
    public record BloqueDto(BateoDto bateo, PitcheoDto pitcheo) {
    }

    public record EstadisticasJugadorDto(JugadorResumen jugador, BloqueDto temporada, BloqueDto carrera) {
    }

    public record FilaBateoDto(JugadorResumen jugador, EquipoResumen equipo, BateoDto bateo) {
    }

    public record FilaPitcheoDto(JugadorResumen jugador, EquipoResumen equipo, PitcheoDto pitcheo) {
    }

    public record TurnoHistorialDto(Long partidoId, LocalDateTime fecha, int entrada, Mitad mitad,
                                    Jugada.Resultado resultado, String descripcion, int carreras) {
    }

    /** Probabilidades 0..1 (hit, embasarse, ponche) y explicación en lenguaje claro. */
    public record PronosticoDto(BigDecimal hit, BigDecimal embasarse, BigDecimal ponche, int turnosBateador,
                                int bateadoresLanzador, int turnosDirectos, String resumen) {
    }

    /** Historial directo de todas las temporadas; el pronóstico usa la temporada pedida. */
    public record EnfrentamientoDto(JugadorResumen bateador, JugadorResumen lanzador, BateoDto historial,
                                    List<TurnoHistorialDto> turnos, PronosticoDto pronostico) {
    }
}
