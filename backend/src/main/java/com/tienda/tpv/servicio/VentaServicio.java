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
import com.tienda.tpv.dominio.ComponentePack;
import com.tienda.tpv.dto.LineaVentaDTO;
import com.tienda.tpv.repositorio.ComponentePackRepositorio;
import com.tienda.tpv.repositorio.LineaDevolucionRepositorio;
import com.tienda.tpv.repositorio.MovimientoStockRepositorio;
import com.tienda.tpv.repositorio.ProductoRepositorio;
import com.tienda.tpv.repositorio.VentaRepositorio;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class VentaServicio {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final VentaRepositorio ventaRepositorio;
    private final ProductoRepositorio productoRepositorio;
    private final MovimientoStockRepositorio movimientoStockRepositorio;
    private final ComponentePackRepositorio componentePackRepositorio;
    private final LineaDevolucionRepositorio lineaDevolucionRepositorio;
    private final UsuarioActualServicio usuarioActualServicio;

    public VentaServicio(VentaRepositorio ventaRepositorio,
                         ProductoRepositorio productoRepositorio,
                         MovimientoStockRepositorio movimientoStockRepositorio,
                         ComponentePackRepositorio componentePackRepositorio,
                         LineaDevolucionRepositorio lineaDevolucionRepositorio,
                         UsuarioActualServicio usuarioActualServicio) {
        this.ventaRepositorio = ventaRepositorio;
        this.productoRepositorio = productoRepositorio;
        this.movimientoStockRepositorio = movimientoStockRepositorio;
        this.componentePackRepositorio = componentePackRepositorio;
        this.lineaDevolucionRepositorio = lineaDevolucionRepositorio;
        this.usuarioActualServicio = usuarioActualServicio;
    }

    /**
     * Finaliza una venta de caja: valida stock línea a línea, lo descuenta,
     * calcula el total y el IVA contenido (los precios llevan el IVA incluido)
     * y deja un movimiento SALIDA por cada línea.
     */
    public VentaDTO crear(VentaEntradaDTO entrada) {
        var usuario = usuarioActualServicio.obtener();
        Venta venta = new Venta();
        venta.setMetodoPago(entrada.metodoPago());
        venta.setUsuario(usuario);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalIva = BigDecimal.ZERO;
        List<MovimientoStock> movimientosComponentes = new ArrayList<>();

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

            BigDecimal descuento = lineaEntrada.descuentoPorcentaje() != null
                    ? lineaEntrada.descuentoPorcentaje() : BigDecimal.ZERO;
            if (descuento.signum() > 0 && !esAdmin()) {
                throw new AccessDeniedException("Solo un administrador puede aplicar descuentos");
            }

            BigDecimal precioConDescuento = producto.getPrecioVenta()
                    .multiply(CIEN.subtract(descuento))
                    .divide(CIEN, 4, RoundingMode.HALF_UP);
            BigDecimal subtotal = precioConDescuento.multiply(cantidad)
                    .setScale(2, RoundingMode.HALF_UP);
            // IVA contenido en un precio con IVA: subtotal * iva / (100 + iva)
            BigDecimal ivaLinea = subtotal.multiply(producto.getIvaPorcentaje())
                    .divide(CIEN.add(producto.getIvaPorcentaje()), 2, RoundingMode.HALF_UP);

            LineaVenta linea = new LineaVenta();
            linea.setProducto(producto);
            linea.setCantidad(cantidad);
            linea.setPrecioUnitario(producto.getPrecioVenta());
            linea.setDescuentoPorcentaje(descuento);
            linea.setSubtotal(subtotal);
            linea.setIvaPorcentaje(producto.getIvaPorcentaje());
            venta.agregarLinea(linea);

            producto.setStockActual(producto.getStockActual().subtract(cantidad));
            productoRepositorio.flush();

            if (producto.isEsPack()) {
                movimientosComponentes.addAll(descontarComponentesDePack(producto, cantidad));
            }

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
            movimiento.setUsuario(usuario);
            movimientoStockRepositorio.save(movimiento);
        }
        for (MovimientoStock movimiento : movimientosComponentes) {
            movimiento.setMotivo(movimiento.getMotivo() + " — venta #" + venta.getId());
            movimiento.setUsuario(usuario);
            movimientoStockRepositorio.save(movimiento);
        }
        return VentaDTO.desde(venta);
    }

    /**
     * Al vender un pack se descuenta también el stock de sus componentes,
     * proporcionalmente a la cantidad de packs vendidos. Devuelve los
     * movimientos SALIDA (con el motivo aún sin el número de venta) para
     * guardarlos una vez la venta tenga id.
     */
    private List<MovimientoStock> descontarComponentesDePack(Producto pack, BigDecimal cantidadPacksVendidos) {
        List<MovimientoStock> movimientos = new ArrayList<>();
        for (ComponentePack relacion : componentePackRepositorio.findByPackId(pack.getId())) {
            Producto componente = relacion.getComponente();
            BigDecimal cantidadNecesaria = relacion.getCantidad().multiply(cantidadPacksVendidos);
            if (componente.getStockActual().compareTo(cantidadNecesaria) < 0) {
                throw new StockInsuficienteException(
                        "Stock insuficiente de '" + componente.getNombre() + "' (componente de '" + pack.getNombre()
                                + "'): quedan " + componente.getStockActual().stripTrailingZeros().toPlainString());
            }
            componente.setStockActual(componente.getStockActual().subtract(cantidadNecesaria));
            productoRepositorio.flush();

            MovimientoStock movimiento = new MovimientoStock();
            movimiento.setProducto(componente);
            movimiento.setTipo(TipoMovimiento.SALIDA);
            movimiento.setCantidad(cantidadNecesaria.negate());
            movimiento.setMotivo("Componente del pack '" + pack.getNombre() + "'");
            movimientos.add(movimiento);
        }
        return movimientos;
    }

    /** Incluye, por línea, cuánto se ha devuelto ya (para saber qué queda devolvible). */
    @Transactional(readOnly = true)
    public VentaDTO obtener(Long id) {
        Venta venta = ventaRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la venta con id " + id));
        List<LineaVentaDTO> lineas = venta.getLineas().stream()
                .map(l -> LineaVentaDTO.desde(l, lineaDevolucionRepositorio.sumCantidadPorLineaVenta(l.getId())))
                .toList();
        return new VentaDTO(venta.getId(), venta.getFechaHora(), venta.getTotal(), venta.getTotalIva(),
                venta.getMetodoPago(), lineas, venta.getUsuario() != null ? venta.getUsuario().getNombre() : null);
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

    private boolean esAdmin() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
