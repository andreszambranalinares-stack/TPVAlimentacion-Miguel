package com.tienda.tpv.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Línea del carrito al crear una venta. El descuento es opcional (null o 0 = sin descuento)
 * y su uso está reservado a ADMIN; se valida en VentaServicio.
 */
public record LineaVentaEntradaDTO(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0", inclusive = false, message = "La cantidad debe ser mayor que cero")
        BigDecimal cantidad,

        @DecimalMin(value = "0", message = "El descuento no puede ser negativo")
        @DecimalMax(value = "100", message = "El descuento no puede ser mayor de 100")
        BigDecimal descuentoPorcentaje
) {
}
