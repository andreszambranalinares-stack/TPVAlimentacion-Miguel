package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.RolUsuario;
import com.tienda.tpv.dominio.Usuario;

/** Usuario visto por un administrador en la pantalla de gestión de empleados. */
public record UsuarioAdminDTO(Long id, String nombreUsuario, String nombre, RolUsuario rol, boolean activo) {

    public static UsuarioAdminDTO desde(Usuario u) {
        return new UsuarioAdminDTO(u.getId(), u.getNombreUsuario(), u.getNombre(), u.getRol(), u.isActivo());
    }
}
