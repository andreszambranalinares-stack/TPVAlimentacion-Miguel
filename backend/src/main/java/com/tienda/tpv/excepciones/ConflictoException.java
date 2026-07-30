package com.tienda.tpv.excepciones;

/** Se lanza ante conflictos con datos existentes, como duplicados (respuesta 409). */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
