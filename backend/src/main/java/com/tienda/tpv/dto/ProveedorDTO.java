package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.Proveedor;

/** Datos de salida de un proveedor. */
public record ProveedorDTO(Long id, String nombre, String telefono, String contacto) {

    public static ProveedorDTO desde(Proveedor p) {
        return new ProveedorDTO(p.getId(), p.getNombre(), p.getTelefono(), p.getContacto());
    }
}
