package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.RolUsuario;
import com.tienda.tpv.dominio.Usuario;
import com.tienda.tpv.dto.CambiarMiPasswordDTO;
import com.tienda.tpv.dto.NuevaPasswordDTO;
import com.tienda.tpv.dto.UsuarioActualizarDTO;
import com.tienda.tpv.dto.UsuarioAdminDTO;
import com.tienda.tpv.dto.UsuarioEntradaDTO;
import com.tienda.tpv.excepciones.ConflictoException;
import com.tienda.tpv.excepciones.RecursoNoEncontradoException;
import com.tienda.tpv.excepciones.ValidacionException;
import com.tienda.tpv.repositorio.UsuarioRepositorio;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UsuarioServicio {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder codificadorPasswords;

    public UsuarioServicio(UsuarioRepositorio usuarioRepositorio, PasswordEncoder codificadorPasswords) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.codificadorPasswords = codificadorPasswords;
    }

    @Transactional(readOnly = true)
    public List<UsuarioAdminDTO> listar() {
        return usuarioRepositorio.findAll(Sort.by("nombre")).stream()
                .map(UsuarioAdminDTO::desde)
                .toList();
    }

    public UsuarioAdminDTO crear(UsuarioEntradaDTO entrada) {
        String nombreUsuario = entrada.nombreUsuario().trim();
        if (usuarioRepositorio.existsByNombreUsuario(nombreUsuario)) {
            throw new ConflictoException("Ya existe un usuario con el nombre '" + nombreUsuario + "'");
        }
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setHashPassword(codificadorPasswords.encode(entrada.password()));
        usuario.setNombre(entrada.nombre().trim());
        usuario.setRol(entrada.rol());
        usuario.setActivo(true);
        return UsuarioAdminDTO.desde(usuarioRepositorio.save(usuario));
    }

    public UsuarioAdminDTO actualizar(Long id, UsuarioActualizarDTO entrada) {
        Usuario usuario = buscarPorId(id);
        if (usuario.getRol() == RolUsuario.ADMIN && usuario.isActivo()
                && (entrada.rol() != RolUsuario.ADMIN || !entrada.activo())) {
            long otrosAdminsActivos = usuarioRepositorio.countByRolAndActivoTrueAndIdNot(RolUsuario.ADMIN, id);
            if (otrosAdminsActivos == 0) {
                throw new ValidacionException(
                        "No puedes desactivar ni quitar el rol de administrador al único administrador activo");
            }
        }
        usuario.setNombre(entrada.nombre().trim());
        usuario.setRol(entrada.rol());
        usuario.setActivo(entrada.activo());
        return UsuarioAdminDTO.desde(usuario);
    }

    public void cambiarPassword(Long id, NuevaPasswordDTO entrada) {
        Usuario usuario = buscarPorId(id);
        usuario.setHashPassword(codificadorPasswords.encode(entrada.password()));
    }

    public void cambiarMiPassword(String nombreUsuario, CambiarMiPasswordDTO entrada) {
        Usuario usuario = usuarioRepositorio.findByNombreUsuarioAndActivoTrue(nombreUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + nombreUsuario));
        if (!codificadorPasswords.matches(entrada.passwordActual(), usuario.getHashPassword())) {
            throw new ValidacionException("La contraseña actual no es correcta");
        }
        usuario.setHashPassword(codificadorPasswords.encode(entrada.passwordNueva()));
    }

    private Usuario buscarPorId(Long id) {
        return usuarioRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario con id " + id));
    }
}
