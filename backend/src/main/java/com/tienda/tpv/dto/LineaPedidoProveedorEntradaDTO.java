package com.tienda.tpv.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LineaPedidoProveedorEntradaDTO(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0", inclusive = false, message = "La cantidad debe ser mayor que cero")
        BigDecimal cantidad,

        @DecimalMin(value = "0", message = "El precio de coste no puede ser negativo")
        BigDecimal precioCosteUnitario
) {
}
