package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.MetodoPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Datos para finalizar una venta desde la caja. */
public record VentaEntradaDTO(
        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodoPago,

        @NotEmpty(message = "La venta debe tener al menos una línea")
        List<@Valid LineaVentaEntradaDTO> lineas
) {
}
