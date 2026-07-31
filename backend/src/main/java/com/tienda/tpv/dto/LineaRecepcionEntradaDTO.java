package com.tienda.tpv.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LineaRecepcionEntradaDTO(
        @NotNull(message = "La línea del pedido es obligatoria")
        Long lineaPedidoId,

        @NotNull(message = "La cantidad recibida es obligatoria")
        @DecimalMin(value = "0", inclusive = false, message = "La cantidad recibida debe ser mayor que cero")
        BigDecimal cantidadRecibida
) {
}
