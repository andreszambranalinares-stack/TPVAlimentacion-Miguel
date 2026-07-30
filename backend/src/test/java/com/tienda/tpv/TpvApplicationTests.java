package com.tienda.tpv;

import com.tienda.tpv.dominio.Categoria;
import com.tienda.tpv.dominio.Producto;
import com.tienda.tpv.dominio.UnidadMedida;
import com.tienda.tpv.repositorio.CategoriaRepositorio;
import com.tienda.tpv.repositorio.ProductoRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que el contexto arranca, que Flyway aplica la migración sobre H2
 * y que las entidades encajan con el esquema (ddl-auto: validate).
 */
@SpringBootTest
@ActiveProfiles("test")
class TpvApplicationTests {

    @Autowired
    private CategoriaRepositorio categoriaRepositorio;

    @Autowired
    private ProductoRepositorio productoRepositorio;

    @Test
    void elContextoArrancaYSePuedePersistir() {
        Categoria bebidas = categoriaRepositorio.save(new Categoria("Bebidas"));

        Producto producto = new Producto();
        producto.setNombre("Agua mineral 1,5L");
        producto.setCodigoBarras("8410000000017");
        producto.setCategoria(bebidas);
        producto.setPrecioVenta(new BigDecimal("0.60"));
        producto.setPrecioCoste(new BigDecimal("0.30"));
        producto.setIvaPorcentaje(new BigDecimal("10"));
        producto.setStockActual(new BigDecimal("24"));
        producto.setStockMinimo(new BigDecimal("6"));
        producto.setUnidadMedida(UnidadMedida.UNIDAD);
        productoRepositorio.save(producto);

        assertThat(productoRepositorio.findByCodigoBarras("8410000000017"))
                .isPresent()
                .get()
                .satisfies(p -> assertThat(p.getNombre()).isEqualTo("Agua mineral 1,5L"));

        assertThat(productoRepositorio.buscarActivos("agua")).hasSize(1);
        assertThat(productoRepositorio.findBajoStockMinimo()).isEmpty();
    }
}
