package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.LineaVenta;
import com.tienda.tpv.dominio.MovimientoStock;
import com.tienda.tpv.dominio.Producto;
import com.tienda.tpv.dominio.TipoMovimiento;
import com.tienda.tpv.dominio.Venta;
import com.tienda.tpv.dto.LineaVentaEntradaDTO;
import com.tienda.tpv.dto.VentaDTO;
import com.tienda.tpv.dto.VentaEntradaDTO;
import com.tienda.tpv.excepciones.RecursoNoEncontradoException;
import com.tienda.tpv.excepciones.StockInsuficienteException;
import com.tienda.tpv.excepciones.ValidacionException;
import com.tienda.tpv.repositorio.MovimientoStockRepositorio;
import com.tienda.tpv.repositorio.ProductoRepositorio;
import com.tienda.tpv.repositorio.VentaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class VentaServicio {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final VentaRepositorio ventaRepositorio;
    private final ProductoRepositorio productoRepositorio;
    private final MovimientoStockRepositorio movimientoStockRepositorio;

    public VentaServicio(VentaRepositorio ventaRepositorio,
                         ProductoRepositorio productoRepositorio,
                         MovimientoStockRepositorio movimientoStockRepositorio) {
        this.ventaRepositorio = ventaRepositorio;
        this.productoRepositorio = productoRepositorio;
        this.movimientoStockRepositorio = movimientoStockRepositorio;
    }

    /**
     * Finaliza una venta de caja: valida stock línea a línea, lo descuenta,
     * calcula el total y el IVA contenido (los precios llevan el IVA incluido)
     * y deja un movimiento SALIDA por cada línea.
     */
    public VentaDTO crear(VentaEntradaDTO entrada) {
        Venta venta = new Venta();
        venta.setMetodoPago(entrada.metodoPago());

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalIva = BigDecimal.ZERO;

        for (LineaVentaEntradaDTO lineaEntrada : entrada.lineas()) {
            Producto producto = productoRepositorio.findById(lineaEntrada.productoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe el producto con id " + lineaEntrada.productoId()));
            if (!producto.isActivo()) {
                throw new ValidacionException("El producto '" + producto.getNombre() + "' está dado de baja");
            }

            BigDecimal cantidad = lineaEntrada.cantidad();
            if (producto.getStockActual().compareTo(cantidad) < 0) {
                throw new StockInsuficienteException(
                        "Stock insuficiente de '" + producto.getNombre() + "': quedan "
                                + producto.getStockActual().stripTrailingZeros().toPlainString());
            }

            BigDecimal subtotal = producto.getPrecioVenta().multiply(cantidad)
                    .setScale(2, RoundingMode.HALF_UP);
            // IVA contenido en un precio con IVA: subtotal * iva / (100 + iva)
            BigDecimal ivaLinea = subtotal.multiply(producto.getIvaPorcentaje())
                    .divide(CIEN.add(producto.getIvaPorcentaje()), 2, RoundingMode.HALF_UP);

            LineaVenta linea = new LineaVenta();
            linea.setProducto(producto);
            linea.setCantidad(cantidad);
            linea.setPrecioUnitario(producto.getPrecioVenta());
            linea.setSubtotal(subtotal);
            venta.agregarLinea(linea);

            producto.setStockActual(producto.getStockActual().subtract(cantidad));
            productoRepositorio.flush();
            total = total.add(subtotal);
            totalIva = totalIva.add(ivaLinea);
        }

        venta.setTotal(total);
        venta.setTotalIva(totalIva);
        venta = ventaRepositorio.save(venta);

        for (LineaVenta linea : venta.getLineas()) {
            MovimientoStock movimiento = new MovimientoStock();
            movimiento.setProducto(linea.getProducto());
            movimiento.setTipo(TipoMovimiento.SALIDA);
            movimiento.setCantidad(linea.getCantidad().negate());
            movimiento.setMotivo("Venta #" + venta.getId());
            movimientoStockRepositorio.save(movimiento);
        }
        return VentaDTO.desde(venta);
    }

    @Transactional(readOnly = true)
    public VentaDTO obtener(Long id) {
        return ventaRepositorio.findById(id)
                .map(VentaDTO::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la venta con id " + id));
    }

    /** Ventas de un rango de fechas (ambas incluidas). Sin parámetros: las de hoy. */
    @Transactional(readOnly = true)
    public List<VentaDTO> listar(LocalDate desde, LocalDate hasta) {
        LocalDate d = desde != null ? desde : LocalDate.now();
        LocalDate h = hasta != null ? hasta : d;
        RangoFechasValidador.validar(d, h);
        LocalDateTime inicio = d.atStartOfDay();
        LocalDateTime fin = h.plusDays(1).atStartOfDay();
        return ventaRepositorio.findByFechaHoraBetweenOrderByFechaHoraDesc(inicio, fin).stream()
                .map(VentaDTO::desde)
                .toList();
    }
}
