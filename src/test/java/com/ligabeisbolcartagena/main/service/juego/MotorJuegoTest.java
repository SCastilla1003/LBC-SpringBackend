package com.ligabeisbolcartagena.main.service.juego;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ligabeisbolcartagena.main.api.error.ApiException;
import com.ligabeisbolcartagena.main.dominio.Jugada.Resultado;
import com.ligabeisbolcartagena.main.dominio.Lanzamiento.Tipo;
import com.ligabeisbolcartagena.main.dominio.Mitad;
import com.ligabeisbolcartagena.main.dominio.Partido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ligabeisbolcartagena.main.dominio.Jugada.Resultado.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Visitantes: jugadores 101..109, lanza 100. Locales: 201..209, lanza 200. */
class MotorJuegoTest {

    static final MotorJuego.Alineaciones ALINEACIONES = new MotorJuego.Alineaciones() {
        @Override
        public Long bateadorEn(boolean local, int orden) {
            return (local ? 200L : 100L) + orden;
        }

        @Override
        public Long lanzadorDe(boolean local) {
            return local ? 200L : 100L;
        }
    };

    EstadoVivo s;

    @BeforeEach
    void iniciar() {
        s = new EstadoVivo();
        s.estado = Partido.Estado.SCHEDULED;
        MotorJuego.iniciar(s, ALINEACIONES);
    }

    void jugada(Resultado r, Movimiento... m) {
        MotorJuego.aplicarJugada(s, r, m.length == 0 ? null : List.of(m), 0, ALINEACIONES);
    }

    @Test
    void alIniciarBateaElPrimeroDelVisitanteYLanzaElLocal() {
        assertThat(s.estado).isEqualTo(Partido.Estado.LIVE);
        assertThat(s.mitad).isEqualTo(Mitad.TOP);
        assertThat(s.bateador).isEqualTo(101L);
        assertThat(s.lanzador).isEqualTo(200L);
    }

    @Test
    void cuatroBolasEsBasePorBolasYTresStrikesPonche() {
        for (int i = 0; i < 3; i++) {
            assertThat(MotorJuego.lanzamiento(s, Tipo.BALL)).isEmpty();
        }
        assertThat(s.bolas).isEqualTo(3);
        assertThat(MotorJuego.lanzamiento(s, Tipo.BALL)).contains(WALK);

        s.bolas = 0;
        MotorJuego.lanzamiento(s, Tipo.CALLED_STRIKE);
        MotorJuego.lanzamiento(s, Tipo.FOUL);
        MotorJuego.lanzamiento(s, Tipo.FOUL); // foul con dos strikes no suma
        assertThat(s.strikes).isEqualTo(2);
        assertThat(MotorJuego.lanzamiento(s, Tipo.SWINGING_STRIKE)).contains(STRIKEOUT);
    }

    @Test
    void sencilloAvanzaCorredoresYPasaAlSiguienteBateador() {
        jugada(SINGLE);
        assertThat(s.bases[1]).isEqualTo(101L);
        assertThat(s.bateador).isEqualTo(102L);
        assertThat(s.hitsVisita).isEqualTo(1);
        jugada(DOUBLE); // 101 de primera a tercera, 102 a segunda
        assertThat(s.bases[3]).isEqualTo(101L);
        assertThat(s.bases[2]).isEqualTo(102L);
        jugada(HOME_RUN);
        assertThat(s.carrerasVisita).isEqualTo(3);
        assertThat(s.bases).containsOnlyNulls();
    }

    @Test
    void basePorBolasSoloMueveCorredoresForzados() {
        s.bases[2] = 150L;
        jugada(WALK);
        assertThat(s.bases[1]).isEqualTo(101L);
        assertThat(s.bases[2]).isEqualTo(150L);
        s.bases[3] = 151L;
        jugada(WALK); // llenas: 101 a segunda, 150 a tercera, 151 anota
        assertThat(s.carrerasVisita).isEqualTo(1);
        assertThat(s.bases[3]).isEqualTo(150L);
    }

    @Test
    void elAnotadorDecideAdondeVaCadaCorredor() {
        s.bases[2] = 150L;
        // Sencillo y el de segunda anota.
        jugada(SINGLE, Movimiento.a(0, 1), Movimiento.a(2, 4));
        assertThat(s.carrerasVisita).isEqualTo(1);
        assertThat(s.bases[1]).isEqualTo(101L);
        assertThat(s.bases[2]).isNull();
    }

    @Test
    void tresOutsCambianLaMitadYElOrdenSigueDondeQuedo() {
        jugada(STRIKEOUT);
        jugada(GROUND_OUT);
        jugada(FLY_OUT);
        assertThat(s.mitad).isEqualTo(Mitad.BOTTOM);
        assertThat(s.outs).isZero();
        assertThat(s.bateador).isEqualTo(201L);
        assertThat(s.lanzador).isEqualTo(100L);
        assertThat(s.proximoVisita).isEqualTo(4);
    }

    @Test
    void lasCarrerasNoCuentanSiLaJugadaHaceElTercerOutForzado() {
        s.outs = 2;
        s.bases[3] = 150L;
        jugada(GROUND_OUT, Movimiento.out(0), Movimiento.a(3, 4));
        assertThat(s.carrerasVisita).isZero();
        assertThat(s.mitad).isEqualTo(Mitad.BOTTOM);
    }

    @Test
    void dobleplayNecesitaDosOuts() {
        s.bases[1] = 150L;
        jugada(DOUBLE_PLAY);
        assertThat(s.outs).isEqualTo(2);
        assertThat(s.bases[1]).isNull();
        assertThatThrownBy(() -> jugada(DOUBLE_PLAY, Movimiento.out(0)))
                .isInstanceOf(ApiException.class).hasMessageContaining("dos outs");
    }

    @Test
    void reglasDeCorredores() {
        s.bases[1] = 150L;
        s.bases[2] = 151L;
        assertThatThrownBy(() -> jugada(SINGLE, Movimiento.a(0, 1), Movimiento.a(1, 3)))
                .hasMessageContaining("adelantar");
        assertThatThrownBy(() -> jugada(DOUBLE, Movimiento.a(0, 1)))
                .hasMessageContaining("segunda");
        assertThatThrownBy(() -> jugada(SINGLE, Movimiento.out(0)))
                .hasMessageContaining("llega a base");
        assertThatThrownBy(() -> jugada(STOLEN_BASE, Movimiento.a(3, 4)))
                .hasMessageContaining("tercera");
        // Robo de base: no cambia el bateador ni el conteo.
        s.bolas = 2;
        jugada(STOLEN_BASE, Movimiento.a(2, 3));
        assertThat(s.bases[3]).isEqualTo(151L);
        assertThat(s.bateador).isEqualTo(101L);
        assertThat(s.bolas).isEqualTo(2);
    }

    @Test
    void elLocalGanaEnLaBajaDeLaUltimaYTerminaElPartido() {
        s.entradasProgramadas = 1;
        jugada(STRIKEOUT);
        jugada(STRIKEOUT);
        jugada(STRIKEOUT);
        assertThat(s.mitad).isEqualTo(Mitad.BOTTOM);
        jugada(HOME_RUN); // dejando en el terreno
        assertThat(s.estado).isEqualTo(Partido.Estado.FINISHED);
        assertThat(s.carrerasLocal).isEqualTo(1);
    }

    @Test
    void siElLocalVaGanandoNoBateaLaBajaDeLaUltima() {
        s.entradasProgramadas = 1;
        s.carrerasLocal = 2;
        jugada(STRIKEOUT);
        jugada(STRIKEOUT);
        jugada(STRIKEOUT);
        assertThat(s.estado).isEqualTo(Partido.Estado.FINISHED);
        assertThat(s.outs).isZero();
    }

    @Test
    void empateEnLaUltimaSigueEnExtraInnings() {
        s.entradasProgramadas = 1;
        for (int i = 0; i < 6; i++) {
            jugada(STRIKEOUT);
        }
        assertThat(s.estado).isEqualTo(Partido.Estado.LIVE);
        assertThat(s.entrada).isEqualTo(2);
        assertThat(s.mitad).isEqualTo(Mitad.TOP);
    }

    @Test
    void laFotoDelEstadoSeGuardaYSeRecuperaEnJson() throws Exception {
        s.bases[2] = 150L;
        s.bolas = 3;
        ObjectMapper om = new ObjectMapper();
        EstadoVivo copia = om.readValue(om.writeValueAsString(s), EstadoVivo.class);
        assertThat(copia.bases[2]).isEqualTo(150L);
        assertThat(copia.bolas).isEqualTo(3);
        assertThat(copia.estado).isEqualTo(Partido.Estado.LIVE);
    }

    @Test
    void conElPartidoSinEmpezarNoSeAnota() {
        EstadoVivo nuevo = new EstadoVivo();
        nuevo.estado = Partido.Estado.SCHEDULED;
        assertThatThrownBy(() -> MotorJuego.lanzamiento(nuevo, Tipo.BALL)).hasMessageContaining("no está en vivo");
    }

    // ------------------------------------------------------------------ walk off y muerte súbita

    /** Baja de la última entrada (de 1) con el marcador [visita]-[local] y las bases dadas. */
    void bajaDeLaUltima(int visita, int local, Long primera, Long segunda, Long tercera) {
        s.entradasProgramadas = 1;
        for (int i = 0; i < 3; i++) {
            jugada(STRIKEOUT);
        }
        assertThat(s.mitad).isEqualTo(Mitad.BOTTOM);
        s.carrerasVisita = visita;
        s.carrerasLocal = local;
        s.bases[1] = primera;
        s.bases[2] = segunda;
        s.bases[3] = tercera;
    }

    @Test
    void walkOffConSencilloSoloCuentaLaCarreraDelTriunfo() {
        bajaDeLaUltima(3, 3, 250L, 251L, 252L);
        MotorJuego.Resumen r = MotorJuego.aplicarJugada(s, DOUBLE, null, 0, ALINEACIONES);
        assertThat(s.estado).isEqualTo(Partido.Estado.FINISHED);
        assertThat(s.finalEspecial).isEqualTo(Partido.FinalEspecial.WALK_OFF);
        assertThat(s.carrerasLocal).isEqualTo(4);
        assertThat(r.carreras()).isEqualTo(1);
        assertThat(r.anotaron()).containsExactly(252L); // el de tercera es el que entra
        assertThat(r.movimientos()).filteredOn(m -> !m.esOut() && m.hasta() == 4).hasSize(1);
    }

    @Test
    void walkOffDesdeAbajoCuentaLasQueHacenFaltaParaGanar() {
        bajaDeLaUltima(5, 4, 250L, 251L, 252L);
        MotorJuego.Resumen r = MotorJuego.aplicarJugada(s, TRIPLE, null, 0, ALINEACIONES);
        assertThat(r.carreras()).isEqualTo(2);
        assertThat(s.carrerasLocal).isEqualTo(6);
        assertThat(s.finalEspecial).isEqualTo(Partido.FinalEspecial.WALK_OFF);
    }

    @Test
    void walkOffGrandSlamAnotanLosCuatro() {
        bajaDeLaUltima(5, 3, 250L, 251L, 252L);
        MotorJuego.Resumen r = MotorJuego.aplicarJugada(s, HOME_RUN, null, 0, ALINEACIONES);
        assertThat(r.carreras()).isEqualTo(4);
        assertThat(s.carrerasLocal).isEqualTo(7);
        assertThat(s.finalEspecial).isEqualTo(Partido.FinalEspecial.WALK_OFF_GRAND_SLAM);
    }

    @Test
    void walkOffHomeRunSolitario() {
        bajaDeLaUltima(2, 2, null, null, null);
        jugada(HOME_RUN);
        assertThat(s.estado).isEqualTo(Partido.Estado.FINISHED);
        assertThat(s.finalEspecial).isEqualTo(Partido.FinalEspecial.WALK_OFF_HOME_RUN);
    }

    @Test
    void walkOffConWildPitchYElevadoDeSacrificio() {
        bajaDeLaUltima(1, 1, 250L, null, 252L);
        MotorJuego.Resumen r = MotorJuego.aplicarJugada(s, WILD_PITCH, null, 0, ALINEACIONES);
        assertThat(r.carreras()).isEqualTo(1);
        assertThat(s.finalEspecial).isEqualTo(Partido.FinalEspecial.WALK_OFF);

        iniciar();
        bajaDeLaUltima(0, 0, null, null, 252L);
        jugada(SACRIFICE_FLY);
        assertThat(s.estado).isEqualTo(Partido.Estado.FINISHED);
        assertThat(s.carrerasLocal).isEqualTo(1);
        assertThat(s.finalEspecial).isEqualTo(Partido.FinalEspecial.WALK_OFF);
    }

    @Test
    void siElLocalSoloEmpataNoSeAcaba() {
        bajaDeLaUltima(2, 1, null, null, 252L);
        jugada(SINGLE);
        assertThat(s.estado).isEqualTo(Partido.Estado.LIVE);
        assertThat(s.finalEspecial).isNull();
    }

    @Test
    void muerteSubitaPoneEnSegundaAlUltimoQueBateo() {
        s.entradasProgramadas = 1;
        s.basesMuerteSubita = new int[]{2};
        jugada(SINGLE);    // 101
        jugada(STRIKEOUT); // 102
        jugada(STRIKEOUT); // 103
        jugada(STRIKEOUT); // 104: fin de la alta, el próximo visitante es el 5
        for (int i = 0; i < 3; i++) {
            jugada(STRIKEOUT); // 201, 202, 203
        }
        assertThat(s.entrada).isEqualTo(2);
        assertThat(s.mitad).isEqualTo(Mitad.TOP);
        assertThat(s.bateador).isEqualTo(105L);
        assertThat(s.bases[2]).isEqualTo(104L);
        assertThat(s.bases[1]).isNull();
        assertThat(s.bases[3]).isNull();
        for (int i = 0; i < 3; i++) {
            jugada(STRIKEOUT); // 105, 106, 107
        }
        assertThat(s.mitad).isEqualTo(Mitad.BOTTOM);
        assertThat(s.bases[2]).isEqualTo(203L); // el local también
    }

    @Test
    void muerteSubitaConDosCorredoresElDeAdelanteEsElUltimoQueBateo() {
        s.entradasProgramadas = 1;
        s.basesMuerteSubita = new int[]{1, 2};
        for (int i = 0; i < 6; i++) {
            jugada(STRIKEOUT); // visitantes 101-103, locales 201-203
        }
        assertThat(s.bases[2]).isEqualTo(103L);
        assertThat(s.bases[1]).isEqualTo(102L);
    }

    @Test
    void muerteSubitaDaLaVueltaAlOrden() {
        s.entradasProgramadas = 1;
        s.basesMuerteSubita = new int[]{2, 3};
        s.proximoVisita = 8;
        s.bateador = 108L;
        for (int i = 0; i < 6; i++) {
            jugada(STRIKEOUT); // visitantes 108, 109, 101 | locales 201, 202, 203
        }
        // En la 2 abre el 102: en tercera el último que bateó (101) y en segunda el anterior (109).
        assertThat(s.bateador).isEqualTo(102L);
        assertThat(s.bases[3]).isEqualTo(101L);
        assertThat(s.bases[2]).isEqualTo(109L);
    }

    @Test
    void sinMuerteSubitaConfiguradaLasBasesEmpiezanVacias() {
        s.entradasProgramadas = 1;
        for (int i = 0; i < 6; i++) {
            jugada(STRIKEOUT);
        }
        assertThat(s.bases).containsOnlyNulls();
    }
}
