package com.tienda.tpv.repositorio;

import com.tienda.tpv.dominio.MovimientoStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoStockRepositorio extends JpaRepository<MovimientoStock, Long> {

    Page<MovimientoStock> findByProductoIdOrderByFechaHoraDesc(Long productoId, Pageable pageable);
}
