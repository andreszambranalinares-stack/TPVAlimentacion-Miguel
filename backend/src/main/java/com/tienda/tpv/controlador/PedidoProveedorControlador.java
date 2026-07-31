package com.tienda.tpv.controlador;

import com.tienda.tpv.dominio.EstadoPedido;
import com.tienda.tpv.dto.PedidoProveedorDTO;
import com.tienda.tpv.dto.PedidoProveedorEntradaDTO;
import com.tienda.tpv.dto.RecepcionEntradaDTO;
import com.tienda.tpv.servicio.PedidoProveedorPdfServicio;
import com.tienda.tpv.servicio.PedidoProveedorServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos-proveedor")
@Tag(name = "Pedidos a proveedor", description = "Pedidos formales con recepción parcial o total")
public class PedidoProveedorControlador {

    private final PedidoProveedorServicio pedidoProveedorServicio;
    private final PedidoProveedorPdfServicio pedidoProveedorPdfServicio;

    public PedidoProveedorControlador(PedidoProveedorServicio pedidoProveedorServicio,
                                      PedidoProveedorPdfServicio pedidoProveedorPdfServicio) {
        this.pedidoProveedorServicio = pedidoProveedorServicio;
        this.pedidoProveedorPdfServicio = pedidoProveedorPdfServicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un pedido a proveedor (solo ADMIN)")
    public PedidoProveedorDTO crear(@Valid @RequestBody PedidoProveedorEntradaDTO entrada) {
        return pedidoProveedorServicio.crear(entrada);
    }

    @GetMapping
    @Operation(summary = "Listar pedidos, opcionalmente filtrados por estado o proveedor")
    public List<PedidoProveedorDTO> listar(@RequestParam(required = false) EstadoPedido estado,
                                           @RequestParam(required = false) Long proveedorId) {
        return pedidoProveedorServicio.listar(estado, proveedorId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un pedido con sus líneas")
    public PedidoProveedorDTO obtener(@PathVariable Long id) {
        return pedidoProveedorServicio.obtener(id);
    }

    @PostMapping("/{id}/recibir")
    @Operation(summary = "Registrar la recepción parcial o total de un pedido (cualquier usuario)")
    public PedidoProveedorDTO recibir(@PathVariable Long id, @Valid @RequestBody RecepcionEntradaDTO entrada) {
        return pedidoProveedorServicio.recibir(id, entrada);
    }

    @PostMapping("/{id}/cancelar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancelar un pedido sin recepciones registradas (solo ADMIN)")
    public void cancelar(@PathVariable Long id) {
        pedidoProveedorServicio.cancelar(id);
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Descargar el pedido en PDF para enviarlo al proveedor")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] pdf = pedidoProveedorPdfServicio.generar(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"pedido-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
