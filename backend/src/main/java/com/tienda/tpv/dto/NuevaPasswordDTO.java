package com.tienda.tpv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nueva contraseña que un administrador asigna a otro usuario. */
public record NuevaPasswordDTO(
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
        String password
) {
}
