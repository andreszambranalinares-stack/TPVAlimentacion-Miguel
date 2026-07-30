package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.MetodoPago;
import com.tienda.tpv.dominio.Venta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Venta registrada, con sus líneas (sirve también de base para el ticket). */
public record VentaDTO(
        Long id,
        LocalDateTime fechaHora,
        BigDecimal total,
        BigDecimal totalIva,
        MetodoPago metodoPago,
        List<LineaVentaDTO> lineas
) {

    public static VentaDTO desde(Venta venta) {
        return new VentaDTO(
                venta.getId(),
                venta.getFechaHora(),
                venta.getTotal(),
                venta.getTotalIva(),
                venta.getMetodoPago(),
                venta.getLineas().stream().map(LineaVentaDTO::desde).toList());
    }
}
