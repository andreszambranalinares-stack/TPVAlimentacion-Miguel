package com.tienda.tpv.repositorio;

import com.tienda.tpv.dominio.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProveedorRepositorio extends JpaRepository<Proveedor, Long> {

    List<Proveedor> findByNombreContainingIgnoreCase(String nombre);
}
