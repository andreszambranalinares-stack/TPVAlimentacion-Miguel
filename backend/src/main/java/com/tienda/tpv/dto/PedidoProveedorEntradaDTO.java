package com.tienda.tpv.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PedidoProveedorEntradaDTO(
        @NotNull(message = "El proveedor es obligatorio")
        Long proveedorId,

        @Size(max = 255, message = "Las notas no pueden superar 255 caracteres")
        String notas,

        @NotEmpty(message = "El pedido debe tener al menos una línea")
        List<@Valid LineaPedidoProveedorEntradaDTO> lineas
) {
}
