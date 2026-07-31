package com.tienda.tpv.controlador;

import com.tienda.tpv.dto.DevolucionDTO;
import com.tienda.tpv.dto.DevolucionEntradaDTO;
import com.tienda.tpv.servicio.DevolucionServicio;
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
@RequestMapping("/api/devoluciones")
@Tag(name = "Devoluciones", description = "Anulación parcial o total de ventas ya cobradas")
public class DevolucionControlador {

    private final DevolucionServicio devolucionServicio;

    public DevolucionControlador(DevolucionServicio devolucionServicio) {
        this.devolucionServicio = devolucionServicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Devolver una o varias líneas de una venta (repone stock)")
    public DevolucionDTO crear(@Valid @RequestBody DevolucionEntradaDTO entrada) {
        return devolucionServicio.crear(entrada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una devolución por id")
    public DevolucionDTO obtener(@PathVariable Long id) {
        return devolucionServicio.obtener(id);
    }

    @GetMapping
    @Operation(summary = "Listar devoluciones por rango de fechas (sin parámetros: las de hoy)")
    public List<DevolucionDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return devolucionServicio.listar(desde, hasta);
    }
}
