package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.RolUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos editables de un empleado ya existente (usuario y contraseña no se tocan aquí). */
public record UsuarioActualizarDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @NotNull(message = "El rol es obligatorio")
        RolUsuario rol,

        boolean activo
) {
}
