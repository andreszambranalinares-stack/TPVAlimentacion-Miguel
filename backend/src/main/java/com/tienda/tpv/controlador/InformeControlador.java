package com.tienda.tpv.controlador;

import com.tienda.tpv.dto.InformeVentasDTO;
import com.tienda.tpv.dto.ProductoVendidoDTO;
import com.tienda.tpv.dto.ValorInventarioDTO;
import com.tienda.tpv.servicio.InformeServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/informes")
@Tag(name = "Informes", description = "Resúmenes de ventas e inventario")
public class InformeControlador {

    private final InformeServicio informeServicio;

    public InformeControlador(InformeServicio informeServicio) {
        this.informeServicio = informeServicio;
    }

    @GetMapping("/ventas")
    @Operation(summary = "Resumen de ventas por rango de fechas (sin parámetros: hoy)")
    public InformeVentasDTO resumenVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return informeServicio.resumenVentas(desde, hasta);
    }

    @GetMapping("/productos-mas-vendidos")
    @Operation(summary = "Productos más vendidos por rango de fechas (sin parámetros: hoy)")
    public List<ProductoVendidoDTO> productosMasVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int limite) {
        return informeServicio.productosMasVendidos(desde, hasta, limite);
    }

    @GetMapping("/valor-inventario")
    @Operation(summary = "Valor del inventario activo a precio de coste y de venta")
    public ValorInventarioDTO valorInventario() {
        return informeServicio.valorInventario();
    }
}
