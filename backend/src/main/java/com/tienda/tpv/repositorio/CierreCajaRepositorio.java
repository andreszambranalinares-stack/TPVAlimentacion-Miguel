package com.tienda.tpv.repositorio;

import com.tienda.tpv.dominio.CierreCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CierreCajaRepositorio extends JpaRepository<CierreCaja, Long> {

    boolean existsByFecha(LocalDate fecha);

    Optional<CierreCaja> findByFecha(LocalDate fecha);
}
