package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.Usuario;
import com.tienda.tpv.repositorio.UsuarioRepositorio;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Usuario autenticado que está haciendo la petición actual, para dejar
 * constancia de qué empleado realizó cada venta, devolución, cierre de caja
 * o movimiento de stock.
 */
@Service
public class UsuarioActualServicio {

    private final UsuarioRepositorio usuarioRepositorio;

    public UsuarioActualServicio(UsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    /** Null si no hay usuario autenticado o ya no está activo (no debería pasar en endpoints protegidos). */
    public Usuario obtener() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null) {
            return null;
        }
        return usuarioRepositorio.findByNombreUsuarioAndActivoTrue(autenticacion.getName()).orElse(null);
    }
}
