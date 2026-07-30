package com.tienda.tpv.excepciones;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** Formato único de error de la API. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorRespuesta(String codigo, String mensaje, Map<String, String> detalles) {

    public ErrorRespuesta(String codigo, String mensaje) {
        this(codigo, mensaje, null);
    }
}
