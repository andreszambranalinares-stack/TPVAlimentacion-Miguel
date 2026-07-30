package com.tienda.tpv.controlador;

import com.tienda.tpv.dto.CierreCajaDTO;
import com.tienda.tpv.dto.CierreCajaEntradaDTO;
import com.tienda.tpv.servicio.CierreCajaServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cierres-caja")
@Tag(name = "Cierre de caja", description = "Arqueo diario del efectivo")
public class CierreCajaControlador {

    private final CierreCajaServicio cierreCajaServicio;

    public CierreCajaControlador(CierreCajaServicio cierreCajaServicio) {
        this.cierreCajaServicio = cierreCajaServicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cerrar la caja de un día (sin fecha: hoy). Solo una vez por fecha.")
    public CierreCajaDTO cerrar(@Valid @RequestBody CierreCajaEntradaDTO entrada) {
        return cierreCajaServicio.cerrar(entrada);
    }

    @GetMapping
    @Operation(summary = "Historial de cierres, del más reciente al más antiguo")
    public List<CierreCajaDTO> listar() {
        return cierreCajaServicio.listar();
    }

    @GetMapping("/{fecha}")
    @Operation(summary = "Cierre de una fecha concreta (404 si esa caja no está cerrada)")
    public ResponseEntity<CierreCajaDTO> obtenerPorFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        CierreCajaDTO cierre = cierreCajaServicio.obtenerPorFecha(fecha);
        return cierre != null ? ResponseEntity.ok(cierre) : ResponseEntity.notFound().build();
    }
}
