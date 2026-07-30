package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.RolUsuario;
import com.tienda.tpv.dominio.Usuario;

/** Usuario autenticado, sin datos sensibles. */
public record UsuarioDTO(String nombreUsuario, String nombre, RolUsuario rol) {

    public static UsuarioDTO desde(Usuario usuario) {
        return new UsuarioDTO(usuario.getNombreUsuario(), usuario.getNombre(), usuario.getRol());
    }
}
