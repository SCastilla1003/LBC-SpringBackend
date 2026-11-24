package com.ligabeisbolcartagena.main.service.estadisticas;

import com.ligabeisbolcartagena.main.service.estadisticas.CalculadoraEstadisticas.Bateo;
import com.ligabeisbolcartagena.main.service.estadisticas.CalculadoraEstadisticas.Pitcheo;

/**
 * Probabilidad de que un bateador conecte hit, se embase o se ponche contra un lanzador.
 *
 * 1. Tasas del bateador y del lanzador, acercadas al promedio de la liga cuando hay pocos
 *    datos (con 30 apariciones de "peso" de la liga).
 * 2. Se combinan con el método log5 de Bill James: p = (b·l/L) / (b·l/L + (1−b)(1−l)/(1−L)).
 * 3. Se mezcla con el historial directo entre los dos (con 20 turnos de "peso" del log5).
 */
public final class Pronostico {

    static final double PESO_LIGA = 30;
    static final double PESO_MODELO = 20;

    /** Tasas de la liga (si no hay datos, valores típicos de béisbol aficionado). */
    public record Liga(double avg, double obp, double ponche) {
        public static Liga de(Bateo total) {
            if (total == null || total.pa < 30) {
                return new Liga(0.250, 0.320, 0.200);
            }
            return new Liga(total.avg(), total.obp(), (double) total.so / total.pa);
        }
    }

    public record Resultado(double hit, double embasarse, double ponche) {
    }

    private Pronostico() {
    }

    public static Resultado calcular(Bateo bateador, Pitcheo lanzador, Bateo directo, Liga liga) {
        Bateo b = bateador == null ? new Bateo() : bateador;
        Pitcheo p = lanzador == null ? new Pitcheo() : lanzador;

        double hitB = regresar(b.h, b.ab, liga.avg());
        double hitP = regresar(p.h, p.abContra, liga.avg());
        double basesB = regresar(b.h + b.bb + b.hbp, b.ab + b.bb + b.hbp + b.sf, liga.obp());
        double basesP = regresar(p.h + p.bb + p.hbp, p.bateadores, liga.obp());
        double kB = regresar(b.so, b.pa, liga.ponche());
        double kP = regresar(p.so, p.bateadores, liga.ponche());

        double hit = log5(hitB, hitP, liga.avg());
        double bases = log5(basesB, basesP, liga.obp());
        double k = log5(kB, kP, liga.ponche());

        if (directo != null) {
            hit = mezclar(hit, directo.h, directo.ab);
            bases = mezclar(bases, directo.h + directo.bb + directo.hbp,
                    directo.ab + directo.bb + directo.hbp + directo.sf);
            k = mezclar(k, directo.so, directo.pa);
        }
        return new Resultado(hit, Math.max(bases, hit), k);
    }

    static double regresar(int exitos, int intentos, double liga) {
        return (exitos + liga * PESO_LIGA) / (intentos + PESO_LIGA);
    }

    static double log5(double b, double p, double liga) {
        if (liga <= 0 || liga >= 1) {
            return (b + p) / 2;
        }
        double si = b * p / liga;
        double no = (1 - b) * (1 - p) / (1 - liga);
        return si / (si + no);
    }

    static double mezclar(double modelo, int exitos, int intentos) {
        return (modelo * PESO_MODELO + exitos) / (PESO_MODELO + intentos);
    }
}
