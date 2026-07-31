package com.tienda.tpv.repositorio;

import com.tienda.tpv.dominio.DatosTienda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatosTiendaRepositorio extends JpaRepository<DatosTienda, Long> {
}
