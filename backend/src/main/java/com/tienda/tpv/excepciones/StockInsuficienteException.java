package com.tienda.tpv.excepciones;

/** Se lanza cuando una venta o movimiento dejaría el stock en negativo (respuesta 409). */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
