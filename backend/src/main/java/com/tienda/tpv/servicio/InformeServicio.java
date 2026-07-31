package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.Devolucion;
import com.tienda.tpv.dominio.LineaDevolucion;
import com.tienda.tpv.dominio.LineaVenta;
import com.tienda.tpv.dominio.MetodoPago;
import com.tienda.tpv.dominio.Producto;
import com.tienda.tpv.dominio.Venta;
import com.tienda.tpv.dto.DesgloseIvaDTO;
import com.tienda.tpv.dto.InformeVentasDTO;
import com.tienda.tpv.dto.ProductoVendidoDTO;
import com.tienda.tpv.dto.ValorInventarioDTO;
import com.tienda.tpv.repositorio.DevolucionRepositorio;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Transactional(readOnly = true)
public class InformeServicio {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final VentaRepositorio ventaRepositorio;
    private final LineaVentaRepositorio lineaVentaRepositorio;
    private final ProductoRepositorio productoRepositorio;
    private final DevolucionRepositorio devolucionRepositorio;

    public InformeServicio(VentaRepositorio ventaRepositorio,
                           LineaVentaRepositorio lineaVentaRepositorio,
                           ProductoRepositorio productoRepositorio,
                           DevolucionRepositorio devolucionRepositorio) {
        this.ventaRepositorio = ventaRepositorio;
        this.lineaVentaRepositorio = lineaVentaRepositorio;
        this.productoRepositorio = productoRepositorio;
        this.devolucionRepositorio = devolucionRepositorio;
    }

    /**
     * Resumen neto del rango: a lo vendido se le restan las devoluciones del mismo
     * rango (por el método de pago de la venta original), para que "efectivo" refleje
     * lo que debería haber de verdad en el cajón.
     */
    public InformeVentasDTO resumenVentas(LocalDate desde, LocalDate hasta) {
        LocalDate d = desde != null ? desde : LocalDate.now();
        LocalDate h = hasta != null ? hasta : d;
        validarRango(d, h);

        LocalDateTime inicio = d.atStartOfDay();
        LocalDateTime fin = h.plusDays(1).atStartOfDay();
        List<Venta> ventas = ventaRepositorio.findByFechaHoraBetweenOrderByFechaHoraDesc(inicio, fin);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalIva = BigDecimal.ZERO;
        BigDecimal efectivo = BigDecimal.ZERO;
        BigDecimal tarjeta = BigDecimal.ZERO;
        // tipo de IVA -> [base imponible, cuota], para el desglose fiscal
        Map<BigDecimal, BigDecimal[]> porTipo = new TreeMap<>(Comparator.reverseOrder());
        for (Venta venta : ventas) {
            total = total.add(venta.getTotal());
            totalIva = totalIva.add(venta.getTotalIva());
            if (venta.getMetodoPago() == MetodoPago.EFECTIVO) {
                efectivo = efectivo.add(venta.getTotal());
            } else {
                tarjeta = tarjeta.add(venta.getTotal());
            }
            for (LineaVenta linea : venta.getLineas()) {
                acumularIva(porTipo, linea.getIvaPorcentaje(), linea.getSubtotal());
            }
        }

        for (Devolucion devolucion : devolucionRepositorio.findByFechaHoraBetweenOrderByFechaHoraDesc(inicio, fin)) {
            total = total.subtract(devolucion.getTotal());
            totalIva = totalIva.subtract(devolucion.getTotalIva());
            if (devolucion.getVenta().getMetodoPago() == MetodoPago.EFECTIVO) {
                efectivo = efectivo.subtract(devolucion.getTotal());
            } else {
                tarjeta = tarjeta.subtract(devolucion.getTotal());
            }
            for (LineaDevolucion linea : devolucion.getLineas()) {
                acumularIva(porTipo, linea.getLineaVenta().getIvaPorcentaje(), linea.getImporte().negate());
            }
        }

        List<DesgloseIvaDTO> desglose = porTipo.entrySet().stream()
                .map(e -> new DesgloseIvaDTO(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
        return new InformeVentasDTO(d, h, ventas.size(), total, totalIva, efectivo, tarjeta, desglose);
    }

    /** Suma a "porTipo" la base imponible y la cuota de un importe con IVA incluido (puede ser negativo, p.ej. devoluciones). */
    private void acumularIva(Map<BigDecimal, BigDecimal[]> porTipo, BigDecimal tipoIva, BigDecimal importeConIva) {
        BigDecimal cuota = importeConIva.multiply(tipoIva)
                .divide(CIEN.add(tipoIva), 2, RoundingMode.HALF_UP);
        BigDecimal base = importeConIva.subtract(cuota);
        BigDecimal[] acumulado = porTipo.computeIfAbsent(tipoIva, t -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
        acumulado[0] = acumulado[0].add(base);
        acumulado[1] = acumulado[1].add(cuota);
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

    /** CSV de ventas del rango, con separador ';' y decimales con coma (Excel en español). */
    public String ventasCsv(LocalDate desde, LocalDate hasta) {
        LocalDate d = desde != null ? desde : LocalDate.now();
        LocalDate h = hasta != null ? hasta : d;
        validarRango(d, h);

        StringBuilder csv = new StringBuilder("Ticket;Fecha;Hora;Método de pago;Total;IVA incluido\n");
        List<Venta> ventas = ventaRepositorio.findByFechaHoraBetweenOrderByFechaHoraDesc(
                d.atStartOfDay(), h.plusDays(1).atStartOfDay());
        for (Venta venta : ventas) {
            csv.append(venta.getId()).append(';')
                    .append(venta.getFechaHora().toLocalDate()).append(';')
                    .append(venta.getFechaHora().toLocalTime().withNano(0)).append(';')
                    .append(venta.getMetodoPago()).append(';')
                    .append(decimalEspanol(venta.getTotal())).append(';')
                    .append(decimalEspanol(venta.getTotalIva())).append('\n');
        }
        return csv.toString();
    }

    /** CSV del inventario activo con su valoración. */
    public String inventarioCsv() {
        StringBuilder csv = new StringBuilder(
                "Producto;Código de barras;Categoría;Unidad;Stock;Stock mínimo;Precio coste;PVP;Valor a coste;Valor a PVP\n");
        List<Producto> activos = productoRepositorio.findAll().stream()
                .filter(Producto::isActivo)
                .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                .toList();
        for (Producto p : activos) {
            csv.append(escaparCsv(p.getNombre())).append(';')
                    .append(p.getCodigoBarras() != null ? p.getCodigoBarras() : "").append(';')
                    .append(p.getCategoria() != null ? escaparCsv(p.getCategoria().getNombre()) : "").append(';')
                    .append(p.getUnidadMedida()).append(';')
                    .append(decimalEspanol(p.getStockActual())).append(';')
                    .append(decimalEspanol(p.getStockMinimo())).append(';')
                    .append(decimalEspanol(p.getPrecioCoste())).append(';')
                    .append(decimalEspanol(p.getPrecioVenta())).append(';')
                    .append(decimalEspanol(p.getStockActual().multiply(p.getPrecioCoste())
                            .setScale(2, RoundingMode.HALF_UP))).append(';')
                    .append(decimalEspanol(p.getStockActual().multiply(p.getPrecioVenta())
                            .setScale(2, RoundingMode.HALF_UP))).append('\n');
        }
        return csv.toString();
    }

    private String decimalEspanol(BigDecimal valor) {
        return valor.toPlainString().replace('.', ',');
    }

    private String escaparCsv(String texto) {
        return texto.contains(";") || texto.contains("\"")
                ? '"' + texto.replace("\"", "\"\"") + '"'
                : texto;
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        RangoFechasValidador.validar(desde, hasta);
    }
}
