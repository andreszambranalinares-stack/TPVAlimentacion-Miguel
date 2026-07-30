package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.UnidadMedida;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Datos de entrada para crear o actualizar un producto.
 * El stock actual no se edita por aquí: en el alta puede indicarse
 * {@code stockInicial} (genera un movimiento ENTRADA) y después
 * solo cambia mediante ventas y movimientos de stock.
 */
public record ProductoEntradaDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @Size(max = 50, message = "El código de barras no puede superar 50 caracteres")
        String codigoBarras,

        Long categoriaId,

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio de venta no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El precio de venta tiene demasiados dígitos")
        BigDecimal precioVenta,

        @DecimalMin(value = "0.00", message = "El precio de coste no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El precio de coste tiene demasiados dígitos")
        BigDecimal precioCoste,

        @NotNull(message = "El porcentaje de IVA es obligatorio")
        @DecimalMin(value = "0", message = "El IVA no puede ser negativo")
        @DecimalMax(value = "21", message = "El IVA no puede ser mayor de 21")
        @Digits(integer = 3, fraction = 1, message = "El IVA tiene demasiados dígitos")
        BigDecimal ivaPorcentaje,

        @DecimalMin(value = "0", message = "El stock mínimo no puede ser negativo")
        BigDecimal stockMinimo,

        @DecimalMin(value = "0", message = "El stock inicial no puede ser negativo")
        BigDecimal stockInicial,

        @NotNull(message = "La unidad de medida es obligatoria")
        UnidadMedida unidadMedida,

        Boolean activo
) {
}
