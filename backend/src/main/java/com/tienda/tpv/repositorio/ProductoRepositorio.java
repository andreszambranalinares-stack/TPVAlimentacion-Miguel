package com.tienda.tpv.repositorio;

import com.tienda.tpv.dominio.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepositorio extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigoBarras(String codigoBarras);

    boolean existsByCodigoBarras(String codigoBarras);

    List<Producto> findByCategoriaId(Long categoriaId);

    /** Búsqueda para la caja: por nombre parcial o código de barras exacto, solo activos. */
    @Query("""
            SELECT p FROM Producto p
            WHERE p.activo = TRUE
              AND (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
                   OR p.codigoBarras = :texto)
            """)
    List<Producto> buscarActivos(@Param("texto") String texto);

    /** Productos activos con stock por debajo del mínimo. */
    @Query("SELECT p FROM Producto p WHERE p.activo = TRUE AND p.stockActual < p.stockMinimo")
    List<Producto> findBajoStockMinimo();
}
