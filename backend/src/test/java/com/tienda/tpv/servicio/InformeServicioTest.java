package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.LineaVenta;
import com.tienda.tpv.dominio.MetodoPago;
import com.tienda.tpv.dominio.Producto;
import com.tienda.tpv.dominio.UnidadMedida;
import com.tienda.tpv.dominio.Venta;
import com.tienda.tpv.dto.DesgloseIvaDTO;
import com.tienda.tpv.dto.InformeVentasDTO;
import com.tienda.tpv.dto.ProductoVendidoDTO;
import com.tienda.tpv.dto.ValorInventarioDTO;
import com.tienda.tpv.excepciones.ValidacionException;
import com.tienda.tpv.repositorio.ProductoRepositorio;
import com.tienda.tpv.repositorio.VentaRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba InformeServicio con repositorios reales sobre H2, no mocks:
 * productosMasVendidos delega en una consulta JPQL escrita a mano, así que
 * un mock solo probaría el "pass-through", no que la agrupación/orden sea correcta.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InformeServicioTest {

    @Autowired
    private InformeServicio informeServicio;

    @Autowired
    private ProductoRepositorio productoRepositorio;

    @Autowired
    private VentaRepositorio ventaRepositorio;

    private Producto crearProducto(String nombre, String precioVenta, String precioCoste, String stock, boolean activo) {
        return crearProducto(nombre, precioVenta, precioCoste, "21", stock, activo);
    }

    private Producto crearProducto(String nombre, String precioVenta, String precioCoste, String iva, String stock, boolean activo) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setPrecioVenta(new BigDecimal(precioVenta));
        producto.setPrecioCoste(new BigDecimal(precioCoste));
        producto.setIvaPorcentaje(new BigDecimal(iva));
        producto.setStockActual(new BigDecimal(stock));
        producto.setUnidadMedida(UnidadMedida.UNIDAD);
        producto.setActivo(activo);
        return productoRepositorio.save(producto);
    }

    private void venderHoy(Producto producto, String cantidad, MetodoPago metodoPago) {
        Venta venta = new Venta();
        venta.setMetodoPago(metodoPago);
        LineaVenta linea = new LineaVenta();
        linea.setProducto(producto);
        linea.setCantidad(new BigDecimal(cantidad));
        linea.setPrecioUnitario(producto.getPrecioVenta());
        linea.setSubtotal(producto.getPrecioVenta().multiply(new BigDecimal(cantidad)));
        linea.setIvaPorcentaje(producto.getIvaPorcentaje());
        venta.agregarLinea(linea);
        venta.setTotal(linea.getSubtotal());
        venta.setTotalIva(BigDecimal.ZERO);
        ventaRepositorio.save(venta);
    }

    @Test
    void productosMasVendidosOrdenaPorCantidadYRespetaElLimite() {
        Producto a = crearProducto("Producto A", "1.00", "0.50", "100", true);
        Producto b = crearProducto("Producto B", "2.00", "1.00", "100", true);
        Producto c = crearProducto("Producto C", "3.00", "1.50", "100", true);
        venderHoy(a, "10", MetodoPago.EFECTIVO);
        venderHoy(b, "5", MetodoPago.EFECTIVO);
        venderHoy(c, "1", MetodoPago.TARJETA);

        LocalDate hoy = LocalDate.now();
        List<ProductoVendidoDTO> resultado = informeServicio.productosMasVendidos(hoy, hoy, 2);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).nombre()).isEqualTo("Producto A");
        assertThat(resultado.get(0).cantidadVendida()).isEqualByComparingTo("10");
        assertThat(resultado.get(1).nombre()).isEqualTo("Producto B");
    }

    @Test
    void valorInventarioSumaSoloProductosActivos() {
        crearProducto("Activo 1", "10.00", "5.00", "2", true);
        crearProducto("Activo 2", "20.00", "10.00", "1", true);
        crearProducto("Inactivo", "100.00", "50.00", "100", false);

        ValorInventarioDTO valor = informeServicio.valorInventario();

        assertThat(valor.productosActivos()).isEqualTo(2);
        // 2*5 + 1*10 = 20 a coste; 2*10 + 1*20 = 40 a venta
        assertThat(valor.valorCoste()).isEqualByComparingTo("20.00");
        assertThat(valor.valorVenta()).isEqualByComparingTo("40.00");
    }

    @Test
    void inventarioCsvEscapaNombresConPuntoYComaYComillas() {
        crearProducto("Aceite; oliva \"virgen\"", "5.00", "3.00", "10", true);

        String csv = informeServicio.inventarioCsv();

        assertThat(csv).contains("\"Aceite; oliva \"\"virgen\"\"\"");
    }

    @Test
    void resumenVentasDesglosaElIvaPorTipo() {
        Producto pan = crearProducto("Pan", "1.00", "0.50", "21", "100", true);
        Producto leche = crearProducto("Leche", "1.00", "0.50", "4", "100", true);
        venderHoy(pan, "10", MetodoPago.EFECTIVO);
        venderHoy(leche, "5", MetodoPago.TARJETA);

        LocalDate hoy = LocalDate.now();
        InformeVentasDTO resumen = informeServicio.resumenVentas(hoy, hoy);

        assertThat(resumen.desgloseIva()).hasSize(2);
        DesgloseIvaDTO iva21 = resumen.desgloseIva().get(0);
        assertThat(iva21.tipoIva()).isEqualByComparingTo("21");
        assertThat(iva21.baseImponible()).isEqualByComparingTo("8.26");
        assertThat(iva21.cuotaIva()).isEqualByComparingTo("1.74");

        DesgloseIvaDTO iva4 = resumen.desgloseIva().get(1);
        assertThat(iva4.tipoIva()).isEqualByComparingTo("4");
        assertThat(iva4.baseImponible()).isEqualByComparingTo("4.81");
        assertThat(iva4.cuotaIva()).isEqualByComparingTo("0.19");
    }

    @Test
    void rangoDeFechasSuperiorAlMaximoLanzaValidacion() {
        LocalDate desde = LocalDate.now().minusDays(400);
        LocalDate hasta = LocalDate.now();

        assertThatThrownBy(() -> informeServicio.resumenVentas(desde, hasta))
                .isInstanceOf(ValidacionException.class);
    }
}
