package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.LineaVenta;

import java.math.BigDecimal;

/** Línea de una venta ya registrada. */
public record LineaVentaDTO(
        Long productoId,
        String productoNombre,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {

    public static LineaVentaDTO desde(LineaVenta linea) {
        return new LineaVentaDTO(
                linea.getProducto().getId(),
                linea.getProducto().getNombre(),
                linea.getCantidad(),
                linea.getPrecioUnitario(),
                linea.getSubtotal());
    }
}
