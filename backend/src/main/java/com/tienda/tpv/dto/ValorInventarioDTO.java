package com.tienda.tpv.dto;

import java.math.BigDecimal;

/** Valoración del inventario de productos activos. */
public record ValorInventarioDTO(
        long productosActivos,
        BigDecimal valorCoste,
        BigDecimal valorVenta
) {
}
