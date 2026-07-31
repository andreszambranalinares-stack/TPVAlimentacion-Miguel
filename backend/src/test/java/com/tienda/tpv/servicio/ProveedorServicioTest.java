package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.Proveedor;
import com.tienda.tpv.dto.ProveedorDTO;
import com.tienda.tpv.dto.ProveedorEntradaDTO;
import com.tienda.tpv.excepciones.ConflictoException;
import com.tienda.tpv.excepciones.RecursoNoEncontradoException;
import com.tienda.tpv.repositorio.PedidoProveedorRepositorio;
import com.tienda.tpv.repositorio.ProveedorRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProveedorServicioTest {

    @Mock
    private ProveedorRepositorio proveedorRepositorio;

    @Mock
    private PedidoProveedorRepositorio pedidoProveedorRepositorio;

    @InjectMocks
    private ProveedorServicio proveedorServicio;

    @Test
    void crearGuardaElProveedorConDatosNormalizados() {
        when(proveedorRepositorio.save(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        ProveedorDTO creado = proveedorServicio.crear(
                new ProveedorEntradaDTO("  Droguerías López  ", "912345678", "Marta", "pedidos@droguerias.es", "Calle Mayor 1"));

        assertThat(creado.nombre()).isEqualTo("Droguerías López");
        assertThat(creado.email()).isEqualTo("pedidos@droguerias.es");
        assertThat(creado.direccion()).isEqualTo("Calle Mayor 1");

        ArgumentCaptor<Proveedor> captor = ArgumentCaptor.forClass(Proveedor.class);
        verify(proveedorRepositorio).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Droguerías López");
        assertThat(captor.getValue().getTelefono()).isEqualTo("912345678");
        assertThat(captor.getValue().getEmail()).isEqualTo("pedidos@droguerias.es");
    }

    @Test
    void obtenerDevuelveDTOCorrecto() {
        Proveedor proveedor = new Proveedor("Panadería Central", "600111222", "Juan");
        proveedor.setId(5L);
        when(proveedorRepositorio.findById(5L)).thenReturn(Optional.of(proveedor));

        ProveedorDTO dto = proveedorServicio.obtener(5L);

        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.nombre()).isEqualTo("Panadería Central");
    }

    @Test
    void obtenerProveedorInexistenteLanzaNoEncontrado() {
        when(proveedorRepositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proveedorServicio.obtener(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizarProveedorInexistenteLanzaNoEncontrado() {
        when(proveedorRepositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proveedorServicio.actualizar(99L,
                new ProveedorEntradaDTO("Nuevo nombre", null, null, null, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminarProveedorInexistenteLanzaNoEncontrado() {
        when(proveedorRepositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proveedorServicio.eliminar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void eliminarUnProveedorConPedidosLanzaConflictoYNoLoBorra() {
        Proveedor proveedor = new Proveedor("Coca-Cola", null, null);
        proveedor.setId(7L);
        when(proveedorRepositorio.findById(7L)).thenReturn(Optional.of(proveedor));
        when(pedidoProveedorRepositorio.existsByProveedorId(7L)).thenReturn(true);

        assertThatThrownBy(() -> proveedorServicio.eliminar(7L))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("Coca-Cola");

        verify(proveedorRepositorio, never()).delete(any(Proveedor.class));
    }

    @Test
    void eliminarUnProveedorSinPedidosLoBorra() {
        Proveedor proveedor = new Proveedor("Panadería Central", null, null);
        proveedor.setId(8L);
        when(proveedorRepositorio.findById(8L)).thenReturn(Optional.of(proveedor));
        when(pedidoProveedorRepositorio.existsByProveedorId(8L)).thenReturn(false);

        proveedorServicio.eliminar(8L);

        verify(proveedorRepositorio).delete(proveedor);
    }
}
