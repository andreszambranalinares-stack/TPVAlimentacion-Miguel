package com.tienda.tpv.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Cuántas unidades de un billete/moneda (p. ej. 50, 20, 5, 0.50, 0.05) se han contado. */
public record DenominacionEntradaDTO(
        @NotNull(message = "El valor de la denominación es obligatorio")
        @DecimalMin(value = "0", inclusive = false, message = "El valor debe ser mayor que cero")
        BigDecimal valor,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 0, message = "La cantidad no puede ser negativa")
        Integer cantidad
) {
}
