package com.tienda.tpv.dto;

import java.math.BigDecimal;

/** Ventas netas (venta - devoluciones) de un tipo de IVA, separadas en base imponible y cuota. */
public record DesgloseIvaDTO(
        BigDecimal tipoIva,
        BigDecimal baseImponible,
        BigDecimal cuotaIva
) {
}
