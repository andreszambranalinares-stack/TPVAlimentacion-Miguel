package com.tienda.tpv.repositorio;

import com.tienda.tpv.dominio.Venta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepositorio extends JpaRepository<Venta, Long> {

    List<Venta> findByFechaHoraBetweenOrderByFechaHoraDesc(LocalDateTime desde, LocalDateTime hasta);
}
