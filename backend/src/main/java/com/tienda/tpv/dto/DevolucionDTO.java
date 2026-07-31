package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.Devolucion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DevolucionDTO(
        Long id,
        Long ventaId,
        LocalDateTime fechaHora,
        BigDecimal total,
        BigDecimal totalIva,
        String motivo,
        List<LineaDevolucionDTO> lineas
) {

    public static DevolucionDTO desde(Devolucion d) {
        return new DevolucionDTO(
                d.getId(),
                d.getVenta().getId(),
                d.getFechaHora(),
                d.getTotal(),
                d.getTotalIva(),
                d.getMotivo(),
                d.getLineas().stream().map(LineaDevolucionDTO::desde).toList());
    }
}
