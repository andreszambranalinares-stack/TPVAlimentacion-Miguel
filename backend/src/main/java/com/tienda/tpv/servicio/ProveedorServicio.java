package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.Proveedor;
import com.tienda.tpv.dto.ProveedorDTO;
import com.tienda.tpv.dto.ProveedorEntradaDTO;
import com.tienda.tpv.excepciones.RecursoNoEncontradoException;
import com.tienda.tpv.repositorio.ProveedorRepositorio;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProveedorServicio {

    private final ProveedorRepositorio proveedorRepositorio;

    public ProveedorServicio(ProveedorRepositorio proveedorRepositorio) {
        this.proveedorRepositorio = proveedorRepositorio;
    }

    @Transactional(readOnly = true)
    public List<ProveedorDTO> listar() {
        return proveedorRepositorio.findAll(Sort.by("nombre")).stream()
                .map(ProveedorDTO::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProveedorDTO obtener(Long id) {
        return ProveedorDTO.desde(buscarPorId(id));
    }

    public ProveedorDTO crear(ProveedorEntradaDTO entrada) {
        Proveedor proveedor = new Proveedor(entrada.nombre().trim(), entrada.telefono(), entrada.contacto());
        return ProveedorDTO.desde(proveedorRepositorio.save(proveedor));
    }

    public ProveedorDTO actualizar(Long id, ProveedorEntradaDTO entrada) {
        Proveedor proveedor = buscarPorId(id);
        proveedor.setNombre(entrada.nombre().trim());
        proveedor.setTelefono(entrada.telefono());
        proveedor.setContacto(entrada.contacto());
        return ProveedorDTO.desde(proveedor);
    }

    public void eliminar(Long id) {
        proveedorRepositorio.delete(buscarPorId(id));
    }

    private Proveedor buscarPorId(Long id) {
        return proveedorRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el proveedor con id " + id));
    }
}
