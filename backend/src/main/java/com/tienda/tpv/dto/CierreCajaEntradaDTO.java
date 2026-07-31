package com.tienda.tpv.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Datos para cerrar la caja de un día. Sin fecha se cierra la de hoy.
 * El desglose por denominación es opcional; si se indica, debe sumar
 * exactamente el efectivo contado.
 */
public record CierreCajaEntradaDTO(
        LocalDate fecha,

        @NotNull(message = "El efectivo contado es obligatorio")
        @DecimalMin(value = "0", message = "El efectivo contado no puede ser negativo")
        BigDecimal efectivoContado,

        @Size(max = 255, message = "Las notas no pueden superar 255 caracteres")
        String notas,

        List<@Valid DenominacionEntradaDTO> denominaciones
) {
}
