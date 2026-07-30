package com.tienda.tpv.excepciones;

/** Se lanza cuando el recurso pedido no existe (respuesta 404). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
