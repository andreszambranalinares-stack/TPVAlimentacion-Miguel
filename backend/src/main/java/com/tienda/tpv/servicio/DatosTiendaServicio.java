package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.DatosTienda;
import com.tienda.tpv.dto.DatosTiendaDTO;
import com.tienda.tpv.dto.DatosTiendaEntradaDTO;
import com.tienda.tpv.repositorio.DatosTiendaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DatosTiendaServicio {

    private static final Long ID_FIJO = 1L;

    private final DatosTiendaRepositorio datosTiendaRepositorio;

    public DatosTiendaServicio(DatosTiendaRepositorio datosTiendaRepositorio) {
        this.datosTiendaRepositorio = datosTiendaRepositorio;
    }

    @Transactional(readOnly = true)
    public DatosTiendaDTO obtener() {
        return DatosTiendaDTO.desde(buscar());
    }

    public DatosTiendaDTO actualizar(DatosTiendaEntradaDTO entrada) {
        DatosTienda datos = buscar();
        datos.setNombre(entrada.nombre().trim());
        datos.setDireccion(vacioANulo(entrada.direccion()));
        datos.setTelefono(vacioANulo(entrada.telefono()));
        datos.setNif(vacioANulo(entrada.nif()));
        return DatosTiendaDTO.desde(datos);
    }

    private DatosTienda buscar() {
        // La fila la crea la migración V13; si por lo que sea no existe, se crea aquí.
        return datosTiendaRepositorio.findById(ID_FIJO).orElseGet(() -> {
            DatosTienda nuevo = new DatosTienda();
            nuevo.setId(ID_FIJO);
            nuevo.setNombre("Mi tienda");
            return datosTiendaRepositorio.save(nuevo);
        });
    }

    private String vacioANulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
