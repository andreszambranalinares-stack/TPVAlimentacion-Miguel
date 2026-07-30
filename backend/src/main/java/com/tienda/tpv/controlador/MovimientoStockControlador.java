package com.tienda.tpv.controlador;

import com.tienda.tpv.dto.MovimientoStockDTO;
import com.tienda.tpv.dto.MovimientoStockEntradaDTO;
import com.tienda.tpv.servicio.MovimientoStockServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movimientos-stock")
@Tag(name = "Movimientos de stock", description = "Entradas, salidas, ajustes y mermas con trazabilidad")
public class MovimientoStockControlador {

    private final MovimientoStockServicio movimientoStockServicio;

    public MovimientoStockControlador(MovimientoStockServicio movimientoStockServicio) {
        this.movimientoStockServicio = movimientoStockServicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un movimiento manual (compra, ajuste o merma)")
    public MovimientoStockDTO registrar(@Valid @RequestBody MovimientoStockEntradaDTO entrada) {
        return movimientoStockServicio.registrar(entrada);
    }

    @GetMapping
    @Operation(summary = "Últimos movimientos de un producto")
    public List<MovimientoStockDTO> listar(@RequestParam Long productoId,
                                           @RequestParam(defaultValue = "50") int limite) {
        return movimientoStockServicio.listarPorProducto(productoId, limite);
    }
}
