package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.TipoMovimiento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Registro manual de un movimiento de stock.
 * ENTRADA, SALIDA y MERMA exigen cantidad positiva (el servicio aplica el signo);
 * en AJUSTE la cantidad es el delta con signo (p. ej. -2 tras un recuento).
 */
public record MovimientoStockEntradaDTO(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "El tipo de movimiento es obligatorio")
        TipoMovimiento tipo,

        @NotNull(message = "La cantidad es obligatoria")
        BigDecimal cantidad,

        @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
        String motivo
) {
}
