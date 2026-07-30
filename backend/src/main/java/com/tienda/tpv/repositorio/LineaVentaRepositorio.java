package com.tienda.tpv.repositorio;

import com.tienda.tpv.dominio.LineaVenta;
import com.tienda.tpv.dto.ProductoVendidoDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LineaVentaRepositorio extends JpaRepository<LineaVenta, Long> {

    @Query("""
            SELECT new com.tienda.tpv.dto.ProductoVendidoDTO(
                l.producto.id, l.producto.nombre, SUM(l.cantidad), SUM(l.subtotal))
            FROM LineaVenta l
            WHERE l.venta.fechaHora >= :desde AND l.venta.fechaHora < :hasta
            GROUP BY l.producto.id, l.producto.nombre
            ORDER BY SUM(l.cantidad) DESC
            """)
    List<ProductoVendidoDTO> productosMasVendidos(@Param("desde") LocalDateTime desde,
                                                  @Param("hasta") LocalDateTime hasta,
                                                  Pageable pageable);
}
