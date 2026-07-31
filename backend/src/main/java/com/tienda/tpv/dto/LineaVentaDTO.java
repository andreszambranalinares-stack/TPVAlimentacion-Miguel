package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.LineaVenta;

import java.math.BigDecimal;

/** Línea de una venta ya registrada. */
public record LineaVentaDTO(
        Long id,
        Long productoId,
        String productoNombre,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal descuentoPorcentaje,
        BigDecimal subtotal,
        BigDecimal cantidadDevuelta
) {

    public static LineaVentaDTO desde(LineaVenta linea) {
        return desde(linea, BigDecimal.ZERO);
    }

    public static LineaVentaDTO desde(LineaVenta linea, BigDecimal cantidadDevuelta) {
        return new LineaVentaDTO(
                linea.getId(),
                linea.getProducto().getId(),
                linea.getProducto().getNombre(),
                linea.getCantidad(),
                linea.getPrecioUnitario(),
                linea.getDescuentoPorcentaje(),
                linea.getSubtotal(),
                cantidadDevuelta);
    }
}
