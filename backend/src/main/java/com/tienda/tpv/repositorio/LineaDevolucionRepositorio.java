package com.tienda.tpv.repositorio;

import com.tienda.tpv.dominio.LineaDevolucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface LineaDevolucionRepositorio extends JpaRepository<LineaDevolucion, Long> {

    @Query("SELECT COALESCE(SUM(ld.cantidad), 0) FROM LineaDevolucion ld WHERE ld.lineaVenta.id = :lineaVentaId")
    BigDecimal sumCantidadPorLineaVenta(@Param("lineaVentaId") Long lineaVentaId);
}
