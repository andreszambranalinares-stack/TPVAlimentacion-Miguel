package com.tienda.tpv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambio de la propia contraseña: exige confirmar la actual por seguridad. */
public record CambiarMiPasswordDTO(
        @NotBlank(message = "Debes indicar tu contraseña actual")
        String passwordActual,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 6, max = 72, message = "La nueva contraseña debe tener entre 6 y 72 caracteres")
        String passwordNueva
) {
}
