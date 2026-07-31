package com.tienda.tpv.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RecepcionEntradaDTO(
        @NotEmpty(message = "Indica al menos una línea recibida")
        List<@Valid LineaRecepcionEntradaDTO> lineas
) {
}
