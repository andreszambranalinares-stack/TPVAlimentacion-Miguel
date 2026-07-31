package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.DatosTienda;

/** Datos fiscales/de contacto de la tienda, para mostrar en el ticket. */
public record DatosTiendaDTO(
        String nombre,
        String direccion,
        String telefono,
        String nif
) {

    public static DatosTiendaDTO desde(DatosTienda d) {
        return new DatosTiendaDTO(d.getNombre(), d.getDireccion(), d.getTelefono(), d.getNif());
    }
}
