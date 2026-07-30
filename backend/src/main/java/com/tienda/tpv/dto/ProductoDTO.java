package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.Producto;
import com.tienda.tpv.dominio.UnidadMedida;

import java.math.BigDecimal;

/** Datos de salida de un producto. */
public record ProductoDTO(
        Long id,
        String nombre,
        String codigoBarras,
        Long categoriaId,
        String categoriaNombre,
        BigDecimal precioVenta,
        BigDecimal precioCoste,
        BigDecimal ivaPorcentaje,
        BigDecimal stockActual,
        BigDecimal stockMinimo,
        UnidadMedida unidadMedida,
        boolean activo,
        boolean bajoMinimo
) {

    public static ProductoDTO desde(Producto p) {
        return new ProductoDTO(
                p.getId(),
                p.getNombre(),
                p.getCodigoBarras(),
                p.getCategoria() != null ? p.getCategoria().getId() : null,
                p.getCategoria() != null ? p.getCategoria().getNombre() : null,
                p.getPrecioVenta(),
                p.getPrecioCoste(),
                p.getIvaPorcentaje(),
                p.getStockActual(),
                p.getStockMinimo(),
                p.getUnidadMedida(),
                p.isActivo(),
                p.getStockActual().compareTo(p.getStockMinimo()) < 0
        );
    }
}
