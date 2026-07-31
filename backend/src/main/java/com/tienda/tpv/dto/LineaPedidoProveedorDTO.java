package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.LineaPedidoProveedor;

import java.math.BigDecimal;

public record LineaPedidoProveedorDTO(
        Long id,
        Long productoId,
        String productoNombre,
        BigDecimal cantidadPedida,
        BigDecimal cantidadRecibida,
        BigDecimal cantidadPendiente,
        BigDecimal precioCosteUnitario
) {

    public static LineaPedidoProveedorDTO desde(LineaPedidoProveedor l) {
        return new LineaPedidoProveedorDTO(
                l.getId(),
                l.getProducto().getId(),
                l.getProducto().getNombre(),
                l.getCantidadPedida(),
                l.getCantidadRecibida(),
                l.getCantidadPedida().subtract(l.getCantidadRecibida()),
                l.getPrecioCosteUnitario());
    }
}
