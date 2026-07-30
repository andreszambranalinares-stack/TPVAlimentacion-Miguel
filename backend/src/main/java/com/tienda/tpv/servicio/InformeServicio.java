package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.MetodoPago;
import com.tienda.tpv.dominio.Producto;
import com.tienda.tpv.dominio.Venta;
import com.tienda.tpv.dto.InformeVentasDTO;
import com.tienda.tpv.dto.ProductoVendidoDTO;
import com.tienda.tpv.dto.ValorInventarioDTO;
import com.tienda.tpv.excepciones.ValidacionException;
import com.tienda.tpv.repositorio.LineaVentaRepositorio;
import com.tienda.tpv.repositorio.ProductoRepositorio;
import com.tienda.tpv.repositorio.VentaRepositorio;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InformeServicio {

    private final VentaRepositorio ventaRepositorio;
    private final LineaVentaRepositorio lineaVentaRepositorio;
    private final ProductoRepositorio productoRepositorio;

    public InformeServicio(VentaRepositorio ventaRepositorio,
                           LineaVentaRepositorio lineaVentaRepositorio,
                           ProductoRepositorio productoRepositorio) {
        this.ventaRepositorio = ventaRepositorio;
        this.lineaVentaRepositorio = lineaVentaRepositorio;
        this.productoRepositorio = productoRepositorio;
    }

    public InformeVentasDTO resumenVentas(LocalDate desde, LocalDate hasta) {
        LocalDate d = desde != null ? desde : LocalDate.now();
        LocalDate h = hasta != null ? hasta : d;
        validarRango(d, h);

        List<Venta> ventas = ventaRepositorio.findByFechaHoraBetweenOrderByFechaHoraDesc(
                d.atStartOfDay(), h.plusDays(1).atStartOfDay());

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalIva = BigDecimal.ZERO;
        BigDecimal efectivo = BigDecimal.ZERO;
        BigDecimal tarjeta = BigDecimal.ZERO;
        for (Venta venta : ventas) {
            total = total.add(venta.getTotal());
            totalIva = totalIva.add(venta.getTotalIva());
            if (venta.getMetodoPago() == MetodoPago.EFECTIVO) {
                efectivo = efectivo.add(venta.getTotal());
            } else {
                tarjeta = tarjeta.add(venta.getTotal());
            }
        }
        return new InformeVentasDTO(d, h, ventas.size(), total, totalIva, efectivo, tarjeta);
    }

    public List<ProductoVendidoDTO> productosMasVendidos(LocalDate desde, LocalDate hasta, int limite) {
        LocalDate d = desde != null ? desde : LocalDate.now();
        LocalDate h = hasta != null ? hasta : d;
        validarRango(d, h);
        return lineaVentaRepositorio.productosMasVendidos(
                d.atStartOfDay(), h.plusDays(1).atStartOfDay(), PageRequest.of(0, limite));
    }

    public ValorInventarioDTO valorInventario() {
        List<Producto> activos = productoRepositorio.findAll().stream()
                .filter(Producto::isActivo)
                .toList();
        BigDecimal coste = BigDecimal.ZERO;
        BigDecimal venta = BigDecimal.ZERO;
        for (Producto p : activos) {
            coste = coste.add(p.getStockActual().multiply(p.getPrecioCoste()));
            venta = venta.add(p.getStockActual().multiply(p.getPrecioVenta()));
        }
        return new ValorInventarioDTO(activos.size(),
                coste.setScale(2, RoundingMode.HALF_UP),
                venta.setScale(2, RoundingMode.HALF_UP));
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new ValidacionException("La fecha final no puede ser anterior a la inicial");
        }
    }
}
