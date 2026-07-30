package com.tienda.tpv.servicio;

import com.tienda.tpv.excepciones.ValidacionException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Valida rangos de fechas de consultas (ventas, informes) para evitar consultas sin límite. */
final class RangoFechasValidador {

    static final int RANGO_MAXIMO_DIAS = 366;

    private RangoFechasValidador() {
    }

    static void validar(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new ValidacionException("La fecha final no puede ser anterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > RANGO_MAXIMO_DIAS) {
            throw new ValidacionException("El rango de fechas no puede superar " + RANGO_MAXIMO_DIAS + " días");
        }
    }
}
