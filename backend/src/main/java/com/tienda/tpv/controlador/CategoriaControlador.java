package com.tienda.tpv.controlador;

import com.tienda.tpv.dto.CategoriaDTO;
import com.tienda.tpv.dto.CategoriaEntradaDTO;
import com.tienda.tpv.servicio.CategoriaServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@Tag(name = "Categorías", description = "Gestión de categorías de productos")
public class CategoriaControlador {

    private final CategoriaServicio categoriaServicio;

    public CategoriaControlador(CategoriaServicio categoriaServicio) {
        this.categoriaServicio = categoriaServicio;
    }

    @GetMapping
    @Operation(summary = "Listar todas las categorías ordenadas por nombre")
    public List<CategoriaDTO> listar() {
        return categoriaServicio.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una categoría por id")
    public CategoriaDTO obtener(@PathVariable Long id) {
        return categoriaServicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una categoría")
    public CategoriaDTO crear(@Valid @RequestBody CategoriaEntradaDTO entrada) {
        return categoriaServicio.crear(entrada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar el nombre de una categoría")
    public CategoriaDTO actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaEntradaDTO entrada) {
        return categoriaServicio.actualizar(id, entrada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar una categoría sin productos asociados")
    public void eliminar(@PathVariable Long id) {
        categoriaServicio.eliminar(id);
    }
}
