package com.tienda.tpv.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos de entrada para crear o actualizar un proveedor. */
public record ProveedorEntradaDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
        String telefono,

        @Size(max = 150, message = "El contacto no puede superar 150 caracteres")
        String contacto,

        @Email(message = "El email no es válido")
        @Size(max = 150, message = "El email no puede superar 150 caracteres")
        String email,

        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        String direccion
) {
}
