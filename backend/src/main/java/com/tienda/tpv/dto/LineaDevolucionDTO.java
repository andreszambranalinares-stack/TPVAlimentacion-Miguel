package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.LineaDevolucion;

import java.math.BigDecimal;

public record LineaDevolucionDTO(
        Long id,
        Long lineaVentaId,
        String productoNombre,
        BigDecimal cantidad,
        BigDecimal importe
) {

    public static LineaDevolucionDTO desde(LineaDevolucion linea) {
        return new LineaDevolucionDTO(
                linea.getId(),
                linea.getLineaVenta().getId(),
                linea.getLineaVenta().getProducto().getNombre(),
                linea.getCantidad(),
                linea.getImporte());
    }
}
