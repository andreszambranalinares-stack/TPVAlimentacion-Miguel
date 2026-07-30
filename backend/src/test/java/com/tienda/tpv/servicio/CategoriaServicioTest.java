package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.Categoria;
import com.tienda.tpv.dto.CategoriaDTO;
import com.tienda.tpv.dto.CategoriaEntradaDTO;
import com.tienda.tpv.excepciones.ConflictoException;
import com.tienda.tpv.excepciones.RecursoNoEncontradoException;
import com.tienda.tpv.repositorio.CategoriaRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class CategoriaServicioTest {

    @Mock
    private CategoriaRepositorio categoriaRepositorio;

    @InjectMocks
    private CategoriaServicio categoriaServicio;

    @Test
    void crearCategoriaRecortaElNombreYGuarda() {
        when(categoriaRepositorio.existsByNombreIgnoreCase("Bebidas")).thenReturn(false);
        when(categoriaRepositorio.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaDTO creada = categoriaServicio.crear(new CategoriaEntradaDTO("  Bebidas  "));

        assertThat(creada.nombre()).isEqualTo("Bebidas");
    }

    @Test
    void crearCategoriaConNombreDuplicadoLanzaConflicto() {
        when(categoriaRepositorio.existsByNombreIgnoreCase("Bebidas")).thenReturn(true);

        assertThatThrownBy(() -> categoriaServicio.crear(new CategoriaEntradaDTO("Bebidas")))
                .isInstanceOf(ConflictoException.class);

        verify(categoriaRepositorio, never()).save(any());
    }

    @Test
    void actualizarConNombreDeOtraCategoriaLanzaConflicto() {
        Categoria drogueria = new Categoria("Droguería");
        drogueria.setId(1L);
        Categoria bebidas = new Categoria("Bebidas");
        bebidas.setId(2L);
        when(categoriaRepositorio.findById(1L)).thenReturn(Optional.of(drogueria));
        when(categoriaRepositorio.findByNombreIgnoreCase("Bebidas")).thenReturn(Optional.of(bebidas));

        assertThatThrownBy(() -> categoriaServicio.actualizar(1L, new CategoriaEntradaDTO("Bebidas")))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void obtenerCategoriaInexistenteLanzaNoEncontrado() {
        when(categoriaRepositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaServicio.obtener(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
