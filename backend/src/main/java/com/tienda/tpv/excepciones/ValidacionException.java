package com.tienda.tpv.excepciones;

/** Se lanza cuando los datos de entrada no cumplen reglas de negocio (respuesta 400). */
public class ValidacionException extends RuntimeException {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
