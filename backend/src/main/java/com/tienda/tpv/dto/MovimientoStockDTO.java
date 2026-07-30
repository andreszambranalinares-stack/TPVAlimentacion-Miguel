package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.MovimientoStock;
import com.tienda.tpv.dominio.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Movimiento de stock registrado (cantidad con signo). */
public record MovimientoStockDTO(
        Long id,
        Long productoId,
        String productoNombre,
        TipoMovimiento tipo,
        BigDecimal cantidad,
        String motivo,
        LocalDateTime fechaHora
) {

    public static MovimientoStockDTO desde(MovimientoStock m) {
        return new MovimientoStockDTO(
                m.getId(),
                m.getProducto().getId(),
                m.getProducto().getNombre(),
                m.getTipo(),
                m.getCantidad(),
                m.getMotivo(),
                m.getFechaHora());
    }
}
