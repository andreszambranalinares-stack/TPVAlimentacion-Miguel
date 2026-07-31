package com.tienda.tpv.repositorio;

import com.tienda.tpv.dominio.Devolucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DevolucionRepositorio extends JpaRepository<Devolucion, Long> {

    List<Devolucion> findByFechaHoraBetweenOrderByFechaHoraDesc(LocalDateTime desde, LocalDateTime hasta);
}
