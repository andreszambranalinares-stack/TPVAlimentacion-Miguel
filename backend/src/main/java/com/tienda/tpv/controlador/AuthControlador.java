package com.tienda.tpv.controlador;

import com.tienda.tpv.dto.CredencialesDTO;
import com.tienda.tpv.dto.UsuarioDTO;
import com.tienda.tpv.excepciones.ErrorRespuesta;
import com.tienda.tpv.repositorio.UsuarioRepositorio;
import com.tienda.tpv.servicio.LimitadorAccesoServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Inicio y cierre de sesión")
public class AuthControlador {

    private final AuthenticationManager gestorAutenticacion;
    private final SecurityContextRepository repositorioContexto;
    private final UsuarioRepositorio usuarioRepositorio;
    private final LimitadorAccesoServicio limitadorAcceso;

    public AuthControlador(AuthenticationManager gestorAutenticacion,
                           SecurityContextRepository repositorioContexto,
                           UsuarioRepositorio usuarioRepositorio,
                           LimitadorAccesoServicio limitadorAcceso) {
        this.gestorAutenticacion = gestorAutenticacion;
        this.repositorioContexto = repositorioContexto;
        this.usuarioRepositorio = usuarioRepositorio;
        this.limitadorAcceso = limitadorAcceso;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión con usuario y contraseña (sesión de servidor)")
    public ResponseEntity<Object> login(@Valid @RequestBody CredencialesDTO credenciales,
                                        HttpServletRequest peticion, HttpServletResponse respuesta) {
        if (limitadorAcceso.bloqueado(credenciales.nombreUsuario())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorRespuesta("DEMASIADOS_INTENTOS",
                            "Demasiados intentos fallidos. Inténtalo de nuevo en unos minutos."));
        }
        try {
            Authentication autenticacion = gestorAutenticacion.authenticate(
                    new UsernamePasswordAuthenticationToken(credenciales.nombreUsuario(), credenciales.password()));
            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(autenticacion);
            SecurityContextHolder.setContext(contexto);
            repositorioContexto.saveContext(contexto, peticion, respuesta);
            limitadorAcceso.reiniciar(credenciales.nombreUsuario());
            return ResponseEntity.ok(usuarioActual(autenticacion.getName()));
        } catch (AuthenticationException ex) {
            limitadorAcceso.registrarFallo(credenciales.nombreUsuario());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorRespuesta("CREDENCIALES_INVALIDAS", "Usuario o contraseña incorrectos"));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar la sesión actual")
    public ResponseEntity<Void> logout(HttpServletRequest peticion) {
        HttpSession sesion = peticion.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/yo")
    @Operation(summary = "Usuario de la sesión actual (401 si no hay sesión)")
    public ResponseEntity<Object> yo(Authentication autenticacion) {
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || "anonymousUser".equals(autenticacion.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorRespuesta("NO_AUTENTICADO", "Debes iniciar sesión"));
        }
        return ResponseEntity.ok(usuarioActual(autenticacion.getName()));
    }

    private UsuarioDTO usuarioActual(String nombreUsuario) {
        return usuarioRepositorio.findByNombreUsuarioAndActivoTrue(nombreUsuario)
                .map(UsuarioDTO::desde)
                .orElseThrow();
    }
}
