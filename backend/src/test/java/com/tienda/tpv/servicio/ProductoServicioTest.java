package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.MovimientoStock;
import com.tienda.tpv.dominio.Producto;
import com.tienda.tpv.dominio.TipoMovimiento;
import com.tienda.tpv.dominio.UnidadMedida;
import com.tienda.tpv.dto.ProductoDTO;
import com.tienda.tpv.dto.ProductoEntradaDTO;
import com.tienda.tpv.excepciones.ConflictoException;
import com.tienda.tpv.excepciones.RecursoNoEncontradoException;
import com.tienda.tpv.excepciones.ValidacionException;
import com.tienda.tpv.repositorio.CategoriaRepositorio;
import com.tienda.tpv.repositorio.MovimientoStockRepositorio;
import com.tienda.tpv.repositorio.ProductoRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServicioTest {

    @Mock
    private ProductoRepositorio productoRepositorio;

    @Mock
    private CategoriaRepositorio categoriaRepositorio;

    @Mock
    private MovimientoStockRepositorio movimientoStockRepositorio;

    @InjectMocks
    private ProductoServicio productoServicio;

    private ProductoEntradaDTO entradaValida(String codigoBarras, BigDecimal stockInicial) {
        return new ProductoEntradaDTO("Leche entera 1L", codigoBarras, null,
                new BigDecimal("1.10"), new BigDecimal("0.80"), new BigDecimal("4"),
                new BigDecimal("6"), stockInicial, UnidadMedida.UNIDAD, true);
    }

    @Test
    void crearProductoConStockInicialGeneraMovimientoEntrada() {
        when(productoRepositorio.existsByCodigoBarras("8480000123457")).thenReturn(false);
        when(productoRepositorio.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO creado = productoServicio.crear(entradaValida("8480000123457", new BigDecimal("12")));

        assertThat(creado.nombre()).isEqualTo("Leche entera 1L");
        assertThat(creado.stockActual()).isEqualByComparingTo("12");

        ArgumentCaptor<MovimientoStock> captor = ArgumentCaptor.forClass(MovimientoStock.class);
        verify(movimientoStockRepositorio).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(captor.getValue().getCantidad()).isEqualByComparingTo("12");
    }

    @Test
    void crearProductoConCodigoBarrasDuplicadoLanzaConflicto() {
        when(productoRepositorio.existsByCodigoBarras("8480000123457")).thenReturn(true);

        assertThatThrownBy(() -> productoServicio.crear(entradaValida("8480000123457", null)))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("8480000123457");

        verify(productoRepositorio, never()).save(any());
    }

    @Test
    void crearProductoConIvaInvalidoLanzaValidacion() {
        ProductoEntradaDTO entrada = new ProductoEntradaDTO("Pan", null, null,
                new BigDecimal("0.50"), null, new BigDecimal("7"),
                null, null, UnidadMedida.UNIDAD, true);

        assertThatThrownBy(() -> productoServicio.crear(entrada))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("IVA");
    }

    @Test
    void actualizarProductoInexistenteLanzaNoEncontrado() {
        when(productoRepositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoServicio.actualizar(99L, entradaValida(null, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizarNoModificaElStockActual() {
        Producto existente = new Producto();
        existente.setId(1L);
        existente.setNombre("Leche");
        existente.setStockActual(new BigDecimal("30"));
        when(productoRepositorio.findById(1L)).thenReturn(Optional.of(existente));

        // stockInicial=5 en la entrada debe ignorarse al actualizar
        ProductoDTO actualizado = productoServicio.actualizar(1L, entradaValida(null, new BigDecimal("5")));

        assertThat(actualizado.stockActual()).isEqualByComparingTo("30");
        verify(movimientoStockRepositorio, never()).save(any());
    }
}
