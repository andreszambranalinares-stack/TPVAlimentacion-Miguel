package com.tienda.tpv.servicio;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Bloqueo simple en memoria contra fuerza bruta en el login: tras varios
 * intentos fallidos seguidos con el mismo usuario, se bloquea temporalmente.
 * Pensado para una única instancia del backend (una tienda); no persiste
 * entre reinicios ni se comparte entre instancias.
 */
@Component
public class LimitadorAccesoServicio {

    private static final int MAX_INTENTOS = 5;
    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    private final ConcurrentMap<String, Intento> intentos = new ConcurrentHashMap<>();

    public boolean bloqueado(String usuario) {
        Intento intento = intentos.get(clave(usuario));
        return intento != null && intento.bloqueadoHasta() != null
                && Instant.now().isBefore(intento.bloqueadoHasta());
    }

    public void registrarFallo(String usuario) {
        intentos.compute(clave(usuario), (k, actual) -> {
            int fallos = (actual != null ? actual.fallos() : 0) + 1;
            Instant bloqueadoHasta = fallos >= MAX_INTENTOS ? Instant.now().plus(BLOQUEO) : null;
            return new Intento(fallos, bloqueadoHasta);
        });
    }

    public void reiniciar(String usuario) {
        intentos.remove(clave(usuario));
    }

    private String clave(String usuario) {
        return usuario == null ? "" : usuario.trim().toLowerCase();
    }

    private record Intento(int fallos, Instant bloqueadoHasta) {
    }
}
