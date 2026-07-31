package com.tienda.tpv.controlador;

import com.tienda.tpv.dto.CambiarMiPasswordDTO;
import com.tienda.tpv.dto.NuevaPasswordDTO;
import com.tienda.tpv.dto.UsuarioActualizarDTO;
import com.tienda.tpv.dto.UsuarioAdminDTO;
import com.tienda.tpv.dto.UsuarioEntradaDTO;
import com.tienda.tpv.servicio.UsuarioServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Gestión de empleados con acceso a la aplicación")
public class UsuarioControlador {

    private final UsuarioServicio usuarioServicio;

    public UsuarioControlador(UsuarioServicio usuarioServicio) {
        this.usuarioServicio = usuarioServicio;
    }

    @GetMapping
    @Operation(summary = "Listar empleados (solo ADMIN)")
    public List<UsuarioAdminDTO> listar() {
        return usuarioServicio.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Dar de alta a un empleado con su propio usuario y contraseña (solo ADMIN)")
    public UsuarioAdminDTO crear(@Valid @RequestBody UsuarioEntradaDTO entrada) {
        return usuarioServicio.crear(entrada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar nombre, rol o activar/desactivar a un empleado (solo ADMIN)")
    public UsuarioAdminDTO actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioActualizarDTO entrada) {
        return usuarioServicio.actualizar(id, entrada);
    }

    @PostMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Asignar una nueva contraseña a un empleado (solo ADMIN)")
    public void cambiarPassword(@PathVariable Long id, @Valid @RequestBody NuevaPasswordDTO entrada) {
        usuarioServicio.cambiarPassword(id, entrada);
    }

    @PutMapping("/yo/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cambiar mi propia contraseña (cualquier usuario autenticado)")
    public void cambiarMiPassword(@Valid @RequestBody CambiarMiPasswordDTO entrada, Authentication autenticacion) {
        usuarioServicio.cambiarMiPassword(autenticacion.getName(), entrada);
    }
}
