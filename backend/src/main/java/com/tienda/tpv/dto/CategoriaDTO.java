package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.Categoria;

/** Datos de salida de una categoría. */
public record CategoriaDTO(Long id, String nombre) {

    public static CategoriaDTO desde(Categoria categoria) {
        return new CategoriaDTO(categoria.getId(), categoria.getNombre());
    }
}
