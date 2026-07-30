package com.tienda.tpv.controlador;

import com.tienda.tpv.dto.ProductoDTO;
import com.tienda.tpv.dto.ProductoEntradaDTO;
import com.tienda.tpv.servicio.ProductoServicio;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@Tag(name = "Productos", description = "Gestión del catálogo de productos")
public class ProductoControlador {

    private final ProductoServicio productoServicio;

    public ProductoControlador(ProductoServicio productoServicio) {
        this.productoServicio = productoServicio;
    }

    @GetMapping
    @Operation(summary = "Listar productos",
            description = "Con 'texto' busca por nombre parcial o código de barras exacto (solo activos). "
                    + "Admite filtro por categoría e inclusión de inactivos.")
    public List<ProductoDTO> listar(@RequestParam(required = false) String texto,
                                    @RequestParam(required = false) Long categoriaId,
                                    @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return productoServicio.listar(texto, categoriaId, incluirInactivos);
    }

    @GetMapping("/bajo-minimo")
    @Operation(summary = "Listar productos activos con stock por debajo del mínimo")
    public List<ProductoDTO> listarBajoMinimo() {
        return productoServicio.listarBajoMinimo();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un producto por id")
    public ProductoDTO obtener(@PathVariable Long id) {
        return productoServicio.obtener(id);
    }

    @GetMapping("/codigo-barras/{codigoBarras}")
    @Operation(summary = "Obtener un producto activo por código de barras exacto (lector de la caja)")
    public ProductoDTO obtenerPorCodigoBarras(@PathVariable String codigoBarras) {
        return productoServicio.obtenerPorCodigoBarras(codigoBarras);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un producto",
            description = "Si se indica stockInicial se genera un movimiento ENTRADA con ese stock.")
    public ProductoDTO crear(@Valid @RequestBody ProductoEntradaDTO entrada) {
        return productoServicio.crear(entrada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un producto",
            description = "El stock actual no se modifica por esta vía; usa movimientos de stock.")
    public ProductoDTO actualizar(@PathVariable Long id, @Valid @RequestBody ProductoEntradaDTO entrada) {
        return productoServicio.actualizar(id, entrada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Dar de baja un producto (baja lógica, conserva histórico)")
    public void desactivar(@PathVariable Long id) {
        productoServicio.desactivar(id);
    }
}
