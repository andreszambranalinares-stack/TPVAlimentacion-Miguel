package com.tienda.tpv.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Un producto componente y la cantidad que se necesita por cada unidad del pack. */
public record ComponentePackEntradaDTO(
        @NotNull(message = "El producto componente es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0", inclusive = false, message = "La cantidad debe ser mayor que cero")
        BigDecimal cantidad
) {
}
