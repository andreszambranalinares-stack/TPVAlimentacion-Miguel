package com.tienda.tpv.controlador;

import com.tienda.tpv.dto.DatosTiendaDTO;
import com.tienda.tpv.dto.DatosTiendaEntradaDTO;
import com.tienda.tpv.servicio.DatosTiendaServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/datos-tienda")
@Tag(name = "Datos de la tienda", description = "Datos fiscales/de contacto mostrados en el ticket")
public class DatosTiendaControlador {

    private final DatosTiendaServicio datosTiendaServicio;

    public DatosTiendaControlador(DatosTiendaServicio datosTiendaServicio) {
        this.datosTiendaServicio = datosTiendaServicio;
    }

    @GetMapping
    @Operation(summary = "Obtener los datos de la tienda")
    public DatosTiendaDTO obtener() {
        return datosTiendaServicio.obtener();
    }

    @PutMapping
    @Operation(summary = "Actualizar los datos de la tienda (solo administradores)")
    public DatosTiendaDTO actualizar(@Valid @RequestBody DatosTiendaEntradaDTO entrada) {
        return datosTiendaServicio.actualizar(entrada);
    }
}
