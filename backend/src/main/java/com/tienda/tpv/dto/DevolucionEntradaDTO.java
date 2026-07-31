package com.tienda.tpv.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Devolución (total o parcial) de una venta ya cobrada. */
public record DevolucionEntradaDTO(
        @NotNull(message = "La venta es obligatoria")
        Long ventaId,

        @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
        String motivo,

        @NotEmpty(message = "La devolución debe tener al menos una línea")
        List<@Valid LineaDevolucionEntradaDTO> lineas
) {
}
