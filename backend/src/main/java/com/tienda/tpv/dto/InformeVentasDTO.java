package com.tienda.tpv.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resumen de ventas de un rango de fechas. */
public record InformeVentasDTO(
        LocalDate desde,
        LocalDate hasta,
        long numeroVentas,
        BigDecimal totalVentas,
        BigDecimal totalIva,
        BigDecimal totalEfectivo,
        BigDecimal totalTarjeta
) {
}
