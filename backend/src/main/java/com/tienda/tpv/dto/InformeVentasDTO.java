package com.tienda.tpv.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Resumen de ventas de un rango de fechas. */
public record InformeVentasDTO(
        LocalDate desde,
        LocalDate hasta,
        long numeroVentas,
        BigDecimal totalVentas,
        BigDecimal totalIva,
        BigDecimal totalEfectivo,
        BigDecimal totalTarjeta,
        /** totalIva desglosado por tipo (4%/10%/21%), para la declaración trimestral. */
        List<DesgloseIvaDTO> desgloseIva
) {
}
