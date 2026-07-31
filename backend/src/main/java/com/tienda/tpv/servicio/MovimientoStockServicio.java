package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.MovimientoStock;
import com.tienda.tpv.dominio.Producto;
import com.tienda.tpv.dominio.TipoMovimiento;
import com.tienda.tpv.dto.MovimientoStockDTO;
import com.tienda.tpv.dto.MovimientoStockEntradaDTO;
import com.tienda.tpv.excepciones.RecursoNoEncontradoException;
import com.tienda.tpv.excepciones.StockInsuficienteException;
import com.tienda.tpv.excepciones.ValidacionException;
import com.tienda.tpv.repositorio.MovimientoStockRepositorio;
import com.tienda.tpv.repositorio.ProductoRepositorio;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class MovimientoStockServicio {

    private final MovimientoStockRepositorio movimientoStockRepositorio;
    private final ProductoRepositorio productoRepositorio;

    public MovimientoStockServicio(MovimientoStockRepositorio movimientoStockRepositorio,
                                   ProductoRepositorio productoRepositorio) {
        this.movimientoStockRepositorio = movimientoStockRepositorio;
        this.productoRepositorio = productoRepositorio;
    }

    /**
     * Registra un movimiento manual (compra a proveedor, ajuste de recuento o merma)
     * y actualiza el stock del producto. El stock nunca puede quedar negativo.
     * El ajuste manual (AJUSTE) queda reservado a ADMIN; ENTRADA/SALIDA/MERMA
     * siguen abiertos a cualquier usuario autenticado (recepción de pedidos, mermas diarias).
     */
    @PreAuthorize("hasRole('ADMIN') or #entrada.tipo.name() != 'AJUSTE'")
    public MovimientoStockDTO registrar(MovimientoStockEntradaDTO entrada) {
        Producto producto = productoRepositorio.findById(entrada.productoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el producto con id " + entrada.productoId()));

        BigDecimal delta = calcularDelta(entrada.tipo(), entrada.cantidad());
        BigDecimal nuevoStock = producto.getStockActual().add(delta);
        if (nuevoStock.signum() < 0) {
            throw new StockInsuficienteException(
                    "El movimiento dejaría el stock de '" + producto.getNombre() + "' en negativo (quedan "
                            + producto.getStockActual().stripTrailingZeros().toPlainString() + ")");
        }
        producto.setStockActual(nuevoStock);
        productoRepositorio.flush();

        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setProducto(producto);
        movimiento.setTipo(entrada.tipo());
        movimiento.setCantidad(delta);
        movimiento.setMotivo(entrada.motivo());
        return MovimientoStockDTO.desde(movimientoStockRepositorio.save(movimiento));
    }

    @Transactional(readOnly = true)
    public List<MovimientoStockDTO> listarPorProducto(Long productoId, int limite) {
        if (!productoRepositorio.existsById(productoId)) {
            throw new RecursoNoEncontradoException("No existe el producto con id " + productoId);
        }
        return movimientoStockRepositorio
                .findByProductoIdOrderByFechaHoraDesc(productoId, PageRequest.of(0, limite))
                .map(MovimientoStockDTO::desde)
                .getContent();
    }

    private BigDecimal calcularDelta(TipoMovimiento tipo, BigDecimal cantidad) {
        if (tipo == TipoMovimiento.DEVOLUCION) {
            throw new ValidacionException(
                    "Las devoluciones se registran desde la venta original, no como movimiento manual");
        }
        if (tipo == TipoMovimiento.AJUSTE) {
            if (cantidad.signum() == 0) {
                throw new ValidacionException("El ajuste no puede ser cero");
            }
            return cantidad;
        }
        if (cantidad.signum() <= 0) {
            throw new ValidacionException("La cantidad debe ser mayor que cero");
        }
        return tipo == TipoMovimiento.ENTRADA ? cantidad : cantidad.negate();
    }
}
