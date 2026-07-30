package com.tienda.tpv.dto;

import jakarta.validation.constraints.NotBlank;

/** Credenciales del formulario de acceso. */
public record CredencialesDTO(
        @NotBlank(message = "El usuario es obligatorio")
        String nombreUsuario,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
