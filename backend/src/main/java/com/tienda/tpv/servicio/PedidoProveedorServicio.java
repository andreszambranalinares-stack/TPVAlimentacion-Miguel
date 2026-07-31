package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.EstadoPedido;
import com.tienda.tpv.dominio.LineaPedidoProveedor;
import com.tienda.tpv.dominio.MovimientoStock;
import com.tienda.tpv.dominio.PedidoProveedor;
import com.tienda.tpv.dominio.Producto;
import com.tienda.tpv.dominio.Proveedor;
import com.tienda.tpv.dominio.TipoMovimiento;
import com.tienda.tpv.dto.LineaPedidoProveedorEntradaDTO;
import com.tienda.tpv.dto.LineaRecepcionEntradaDTO;
import com.tienda.tpv.dto.PedidoProveedorDTO;
import com.tienda.tpv.dto.PedidoProveedorEntradaDTO;
import com.tienda.tpv.dto.RecepcionEntradaDTO;
import com.tienda.tpv.excepciones.ConflictoException;
import com.tienda.tpv.excepciones.RecursoNoEncontradoException;
import com.tienda.tpv.excepciones.ValidacionException;
import com.tienda.tpv.repositorio.MovimientoStockRepositorio;
import com.tienda.tpv.repositorio.PedidoProveedorRepositorio;
import com.tienda.tpv.repositorio.ProductoRepositorio;
import com.tienda.tpv.repositorio.ProveedorRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class PedidoProveedorServicio {

    private final PedidoProveedorRepositorio pedidoProveedorRepositorio;
    private final ProveedorRepositorio proveedorRepositorio;
    private final ProductoRepositorio productoRepositorio;
    private final MovimientoStockRepositorio movimientoStockRepositorio;

    public PedidoProveedorServicio(PedidoProveedorRepositorio pedidoProveedorRepositorio,
                                   ProveedorRepositorio proveedorRepositorio,
                                   ProductoRepositorio productoRepositorio,
                                   MovimientoStockRepositorio movimientoStockRepositorio) {
        this.pedidoProveedorRepositorio = pedidoProveedorRepositorio;
        this.proveedorRepositorio = proveedorRepositorio;
        this.productoRepositorio = productoRepositorio;
        this.movimientoStockRepositorio = movimientoStockRepositorio;
    }

    public PedidoProveedorDTO crear(PedidoProveedorEntradaDTO entrada) {
        Proveedor proveedor = proveedorRepositorio.findById(entrada.proveedorId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el proveedor con id " + entrada.proveedorId()));

        PedidoProveedor pedido = new PedidoProveedor();
        pedido.setProveedor(proveedor);
        pedido.setNotas(entrada.notas());
        pedido.setEstado(EstadoPedido.PENDIENTE);

        for (LineaPedidoProveedorEntradaDTO lineaEntrada : entrada.lineas()) {
            Producto producto = productoRepositorio.findById(lineaEntrada.productoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe el producto con id " + lineaEntrada.productoId()));
            LineaPedidoProveedor linea = new LineaPedidoProveedor();
            linea.setProducto(producto);
            linea.setCantidadPedida(lineaEntrada.cantidad());
            linea.setPrecioCosteUnitario(lineaEntrada.precioCosteUnitario());
            pedido.agregarLinea(linea);
        }

        return PedidoProveedorDTO.desde(pedidoProveedorRepositorio.save(pedido));
    }

    @Transactional(readOnly = true)
    public List<PedidoProveedorDTO> listar(EstadoPedido estado, Long proveedorId) {
        List<PedidoProveedor> pedidos;
        if (estado != null) {
            pedidos = pedidoProveedorRepositorio.findByEstadoOrderByFechaHoraDesc(estado);
        } else if (proveedorId != null) {
            pedidos = pedidoProveedorRepositorio.findByProveedorIdOrderByFechaHoraDesc(proveedorId);
        } else {
            pedidos = pedidoProveedorRepositorio.findAllByOrderByFechaHoraDesc();
        }
        return pedidos.stream().map(PedidoProveedorDTO::desde).toList();
    }

    @Transactional(readOnly = true)
    public PedidoProveedorDTO obtener(Long id) {
        return PedidoProveedorDTO.desde(buscarPorId(id));
    }

    /** Registra la recepción (parcial o total) de una o varias líneas del pedido. */
    public PedidoProveedorDTO recibir(Long id, RecepcionEntradaDTO entrada) {
        PedidoProveedor pedido = buscarPorId(id);
        if (pedido.getEstado() == EstadoPedido.CANCELADO || pedido.getEstado() == EstadoPedido.RECIBIDO_COMPLETO) {
            throw new ValidacionException("Este pedido ya está " + estadoLegible(pedido.getEstado())
                    + " y no admite más recepciones");
        }

        for (LineaRecepcionEntradaDTO recepcion : entrada.lineas()) {
            LineaPedidoProveedor linea = pedido.getLineas().stream()
                    .filter(l -> l.getId().equals(recepcion.lineaPedidoId()))
                    .findFirst()
                    .orElseThrow(() -> new ValidacionException(
                            "La línea " + recepcion.lineaPedidoId() + " no pertenece al pedido #" + pedido.getId()));

            BigDecimal pendiente = linea.getCantidadPedida().subtract(linea.getCantidadRecibida());
            if (recepcion.cantidadRecibida().compareTo(pendiente) > 0) {
                throw new ValidacionException(
                        "Solo quedan " + pendiente.stripTrailingZeros().toPlainString()
                                + " unidades pendientes de '" + linea.getProducto().getNombre() + "'");
            }

            linea.setCantidadRecibida(linea.getCantidadRecibida().add(recepcion.cantidadRecibida()));

            Producto producto = linea.getProducto();
            producto.setStockActual(producto.getStockActual().add(recepcion.cantidadRecibida()));
            productoRepositorio.flush();

            MovimientoStock movimiento = new MovimientoStock();
            movimiento.setProducto(producto);
            movimiento.setTipo(TipoMovimiento.ENTRADA);
            movimiento.setCantidad(recepcion.cantidadRecibida());
            movimiento.setMotivo("Recepción del pedido #" + pedido.getId() + " a " + pedido.getProveedor().getNombre());
            movimientoStockRepositorio.save(movimiento);
        }

        boolean todoRecibido = pedido.getLineas().stream()
                .allMatch(l -> l.getCantidadRecibida().compareTo(l.getCantidadPedida()) >= 0);
        boolean algoRecibido = pedido.getLineas().stream()
                .anyMatch(l -> l.getCantidadRecibida().signum() > 0);
        pedido.setEstado(todoRecibido ? EstadoPedido.RECIBIDO_COMPLETO
                : algoRecibido ? EstadoPedido.RECIBIDO_PARCIAL : EstadoPedido.PENDIENTE);

        return PedidoProveedorDTO.desde(pedido);
    }

    public void cancelar(Long id) {
        PedidoProveedor pedido = buscarPorId(id);
        boolean tieneRecepciones = pedido.getLineas().stream()
                .anyMatch(l -> l.getCantidadRecibida().signum() > 0);
        if (tieneRecepciones) {
            throw new ConflictoException("El pedido ya tiene recepciones registradas y no se puede cancelar");
        }
        pedido.setEstado(EstadoPedido.CANCELADO);
    }

    private PedidoProveedor buscarPorId(Long id) {
        return pedidoProveedorRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido con id " + id));
    }

    private String estadoLegible(EstadoPedido estado) {
        return switch (estado) {
            case CANCELADO -> "cancelado";
            case RECIBIDO_COMPLETO -> "recibido por completo";
            default -> estado.name();
        };
    }
}
