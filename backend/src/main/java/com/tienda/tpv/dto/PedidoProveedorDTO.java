package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.EstadoPedido;
import com.tienda.tpv.dominio.PedidoProveedor;

import java.time.LocalDateTime;
import java.util.List;

public record PedidoProveedorDTO(
        Long id,
        Long proveedorId,
        String proveedorNombre,
        LocalDateTime fechaHora,
        EstadoPedido estado,
        String notas,
        List<LineaPedidoProveedorDTO> lineas
) {

    public static PedidoProveedorDTO desde(PedidoProveedor p) {
        return new PedidoProveedorDTO(
                p.getId(),
                p.getProveedor().getId(),
                p.getProveedor().getNombre(),
                p.getFechaHora(),
                p.getEstado(),
                p.getNotas(),
                p.getLineas().stream().map(LineaPedidoProveedorDTO::desde).toList());
    }
}
