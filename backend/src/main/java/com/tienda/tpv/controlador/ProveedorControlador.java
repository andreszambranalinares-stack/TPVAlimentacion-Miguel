package com.tienda.tpv.controlador;

import com.tienda.tpv.dto.ProveedorDTO;
import com.tienda.tpv.dto.ProveedorEntradaDTO;
import com.tienda.tpv.servicio.ProveedorServicio;
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
@RequestMapping("/api/proveedores")
@Tag(name = "Proveedores", description = "Gestión de proveedores")
public class ProveedorControlador {

    private final ProveedorServicio proveedorServicio;

    public ProveedorControlador(ProveedorServicio proveedorServicio) {
        this.proveedorServicio = proveedorServicio;
    }

    @GetMapping
    @Operation(summary = "Listar proveedores ordenados por nombre")
    public List<ProveedorDTO> listar() {
        return proveedorServicio.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un proveedor por id")
    public ProveedorDTO obtener(@PathVariable Long id) {
        return proveedorServicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un proveedor")
    public ProveedorDTO crear(@Valid @RequestBody ProveedorEntradaDTO entrada) {
        return proveedorServicio.crear(entrada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un proveedor")
    public ProveedorDTO actualizar(@PathVariable Long id, @Valid @RequestBody ProveedorEntradaDTO entrada) {
        return proveedorServicio.actualizar(id, entrada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un proveedor")
    public void eliminar(@PathVariable Long id) {
        proveedorServicio.eliminar(id);
    }
}
