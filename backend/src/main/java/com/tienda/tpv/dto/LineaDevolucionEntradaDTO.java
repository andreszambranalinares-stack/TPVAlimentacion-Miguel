package com.tienda.tpv.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Línea de venta que se devuelve y la cantidad devuelta (puede ser parcial). */
public record LineaDevolucionEntradaDTO(
        @NotNull(message = "La línea de venta es obligatoria")
        Long lineaVentaId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0", inclusive = false, message = "La cantidad debe ser mayor que cero")
        BigDecimal cantidad
) {
}
