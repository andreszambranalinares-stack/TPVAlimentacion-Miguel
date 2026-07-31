package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.ComponentePack;
import com.tienda.tpv.dominio.Devolucion;
import com.tienda.tpv.dominio.LineaDevolucion;
import com.tienda.tpv.dominio.LineaVenta;
import com.tienda.tpv.dominio.MovimientoStock;
import com.tienda.tpv.dominio.Producto;
import com.tienda.tpv.dominio.TipoMovimiento;
import com.tienda.tpv.dominio.Venta;
import com.tienda.tpv.dto.DevolucionDTO;
import com.tienda.tpv.dto.DevolucionEntradaDTO;
import com.tienda.tpv.dto.LineaDevolucionEntradaDTO;
import com.tienda.tpv.excepciones.RecursoNoEncontradoException;
import com.tienda.tpv.excepciones.ValidacionException;
import com.tienda.tpv.repositorio.ComponentePackRepositorio;
import com.tienda.tpv.repositorio.DevolucionRepositorio;
import com.tienda.tpv.repositorio.LineaDevolucionRepositorio;
import com.tienda.tpv.repositorio.MovimientoStockRepositorio;
import com.tienda.tpv.repositorio.ProductoRepositorio;
import com.tienda.tpv.repositorio.VentaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Devuelve, parcial o totalmente, líneas de una venta ya cobrada: repone stock
 * (incluidos los componentes si la línea es un pack) y deja movimientos DEVOLUCION.
 */
@Service
@Transactional
public class DevolucionServicio {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final DevolucionRepositorio devolucionRepositorio;
    private final LineaDevolucionRepositorio lineaDevolucionRepositorio;
    private final VentaRepositorio ventaRepositorio;
    private final ProductoRepositorio productoRepositorio;
    private final MovimientoStockRepositorio movimientoStockRepositorio;
    private final ComponentePackRepositorio componentePackRepositorio;

    public DevolucionServicio(DevolucionRepositorio devolucionRepositorio,
                              LineaDevolucionRepositorio lineaDevolucionRepositorio,
                              VentaRepositorio ventaRepositorio,
                              ProductoRepositorio productoRepositorio,
                              MovimientoStockRepositorio movimientoStockRepositorio,
                              ComponentePackRepositorio componentePackRepositorio) {
        this.devolucionRepositorio = devolucionRepositorio;
        this.lineaDevolucionRepositorio = lineaDevolucionRepositorio;
        this.ventaRepositorio = ventaRepositorio;
        this.productoRepositorio = productoRepositorio;
        this.movimientoStockRepositorio = movimientoStockRepositorio;
        this.componentePackRepositorio = componentePackRepositorio;
    }

    public DevolucionDTO crear(DevolucionEntradaDTO entrada) {
        Venta venta = ventaRepositorio.findById(entrada.ventaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la venta con id " + entrada.ventaId()));

        Devolucion devolucion = new Devolucion();
        devolucion.setVenta(venta);
        devolucion.setMotivo(entrada.motivo());

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalIva = BigDecimal.ZERO;
        List<MovimientoStock> movimientos = new ArrayList<>();

        for (LineaDevolucionEntradaDTO lineaEntrada : entrada.lineas()) {
            LineaVenta lineaVenta = venta.getLineas().stream()
                    .filter(l -> l.getId().equals(lineaEntrada.lineaVentaId()))
                    .findFirst()
                    .orElseThrow(() -> new ValidacionException(
                            "La línea " + lineaEntrada.lineaVentaId() + " no pertenece a la venta #" + venta.getId()));

            BigDecimal yaDevuelta = lineaDevolucionRepositorio.sumCantidadPorLineaVenta(lineaVenta.getId());
            BigDecimal disponible = lineaVenta.getCantidad().subtract(yaDevuelta);
            BigDecimal cantidad = lineaEntrada.cantidad();
            if (cantidad.compareTo(disponible) > 0) {
                throw new ValidacionException(
                        "Solo quedan " + disponible.stripTrailingZeros().toPlainString()
                                + " unidades de '" + lineaVenta.getProducto().getNombre() + "' por devolver");
            }

            Producto producto = lineaVenta.getProducto();
            BigDecimal importe = lineaVenta.getPrecioUnitario()
                    .multiply(CIEN.subtract(lineaVenta.getDescuentoPorcentaje()))
                    .divide(CIEN, 4, RoundingMode.HALF_UP)
                    .multiply(cantidad)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal ivaImporte = importe.multiply(producto.getIvaPorcentaje())
                    .divide(CIEN.add(producto.getIvaPorcentaje()), 2, RoundingMode.HALF_UP);

            LineaDevolucion lineaDevolucion = new LineaDevolucion();
            lineaDevolucion.setLineaVenta(lineaVenta);
            lineaDevolucion.setCantidad(cantidad);
            lineaDevolucion.setImporte(importe);
            devolucion.agregarLinea(lineaDevolucion);

            producto.setStockActual(producto.getStockActual().add(cantidad));
            productoRepositorio.flush();
            movimientos.add(crearMovimiento(producto, cantidad, "Devolución de la venta #" + venta.getId()));

            if (producto.isEsPack()) {
                movimientos.addAll(reponerComponentesDePack(producto, cantidad, venta.getId()));
            }

            total = total.add(importe);
            totalIva = totalIva.add(ivaImporte);
        }

        devolucion.setTotal(total);
        devolucion.setTotalIva(totalIva);
        devolucion = devolucionRepositorio.save(devolucion);

        for (MovimientoStock movimiento : movimientos) {
            movimientoStockRepositorio.save(movimiento);
        }
        return DevolucionDTO.desde(devolucion);
    }

    @Transactional(readOnly = true)
    public DevolucionDTO obtener(Long id) {
        return devolucionRepositorio.findById(id)
                .map(DevolucionDTO::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la devolución con id " + id));
    }

    /** Devoluciones de un rango de fechas (ambas incluidas). Sin parámetros: las de hoy. */
    @Transactional(readOnly = true)
    public List<DevolucionDTO> listar(LocalDate desde, LocalDate hasta) {
        LocalDate d = desde != null ? desde : LocalDate.now();
        LocalDate h = hasta != null ? hasta : d;
        RangoFechasValidador.validar(d, h);
        return devolucionRepositorio
                .findByFechaHoraBetweenOrderByFechaHoraDesc(d.atStartOfDay(), h.plusDays(1).atStartOfDay()).stream()
                .map(DevolucionDTO::desde)
                .toList();
    }

    private List<MovimientoStock> reponerComponentesDePack(Producto pack, BigDecimal cantidadPacksDevueltos, Long ventaId) {
        List<MovimientoStock> movimientos = new ArrayList<>();
        for (ComponentePack relacion : componentePackRepositorio.findByPackId(pack.getId())) {
            Producto componente = relacion.getComponente();
            BigDecimal cantidad = relacion.getCantidad().multiply(cantidadPacksDevueltos);
            componente.setStockActual(componente.getStockActual().add(cantidad));
            productoRepositorio.flush();
            movimientos.add(crearMovimiento(componente, cantidad,
                    "Devolución de la venta #" + ventaId + " — componente del pack '" + pack.getNombre() + "'"));
        }
        return movimientos;
    }

    private MovimientoStock crearMovimiento(Producto producto, BigDecimal cantidad, String motivo) {
        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setProducto(producto);
        movimiento.setTipo(TipoMovimiento.DEVOLUCION);
        movimiento.setCantidad(cantidad);
        movimiento.setMotivo(motivo);
        return movimiento;
    }
}
