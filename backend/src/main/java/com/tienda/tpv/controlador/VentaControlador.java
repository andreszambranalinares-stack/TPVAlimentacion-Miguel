package com.tienda.tpv.controlador;

import com.tienda.tpv.dto.VentaDTO;
import com.tienda.tpv.dto.VentaEntradaDTO;
import com.tienda.tpv.servicio.VentaServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@Tag(name = "Ventas", description = "Registro y consulta de ventas de caja")
public class VentaControlador {

    private final VentaServicio ventaServicio;

    public VentaControlador(VentaServicio ventaServicio) {
        this.ventaServicio = ventaServicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Finalizar una venta",
            description = "Valida y descuenta stock, calcula total e IVA y genera movimientos SALIDA.")
    public VentaDTO crear(@Valid @RequestBody VentaEntradaDTO entrada) {
        return ventaServicio.crear(entrada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una venta con sus líneas (datos del ticket)")
    public VentaDTO obtener(@PathVariable Long id) {
        return ventaServicio.obtener(id);
    }

    @GetMapping
    @Operation(summary = "Listar ventas por rango de fechas (sin parámetros: las de hoy)")
    public List<VentaDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ventaServicio.listar(desde, hasta);
    }
}
