package com.tienda.tpv.dto;

import java.math.BigDecimal;

/** Fila del informe de productos más vendidos. */
public record ProductoVendidoDTO(
        Long productoId,
        String nombre,
        BigDecimal cantidadVendida,
        BigDecimal importeTotal
) {
}
