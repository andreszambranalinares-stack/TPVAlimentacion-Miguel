package com.tienda.tpv.repositorio;

import com.tienda.tpv.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByNombreUsuarioAndActivoTrue(String nombreUsuario);
}
