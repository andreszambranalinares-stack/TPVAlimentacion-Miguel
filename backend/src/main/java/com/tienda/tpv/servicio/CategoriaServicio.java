package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.Categoria;
import com.tienda.tpv.dto.CategoriaDTO;
import com.tienda.tpv.dto.CategoriaEntradaDTO;
import com.tienda.tpv.excepciones.ConflictoException;
import com.tienda.tpv.excepciones.RecursoNoEncontradoException;
import com.tienda.tpv.repositorio.CategoriaRepositorio;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoriaServicio {

    private final CategoriaRepositorio categoriaRepositorio;

    public CategoriaServicio(CategoriaRepositorio categoriaRepositorio) {
        this.categoriaRepositorio = categoriaRepositorio;
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTO> listar() {
        return categoriaRepositorio.findAll(Sort.by("nombre")).stream()
                .map(CategoriaDTO::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaDTO obtener(Long id) {
        return CategoriaDTO.desde(buscarPorId(id));
    }

    public CategoriaDTO crear(CategoriaEntradaDTO entrada) {
        String nombre = entrada.nombre().trim();
        if (categoriaRepositorio.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe una categoría con el nombre '" + nombre + "'");
        }
        return CategoriaDTO.desde(categoriaRepositorio.save(new Categoria(nombre)));
    }

    public CategoriaDTO actualizar(Long id, CategoriaEntradaDTO entrada) {
        Categoria categoria = buscarPorId(id);
        String nombre = entrada.nombre().trim();
        categoriaRepositorio.findByNombreIgnoreCase(nombre)
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new ConflictoException("Ya existe una categoría con el nombre '" + nombre + "'");
                });
        categoria.setNombre(nombre);
        return CategoriaDTO.desde(categoria);
    }

    public void eliminar(Long id) {
        Categoria categoria = buscarPorId(id);
        try {
            categoriaRepositorio.delete(categoria);
            categoriaRepositorio.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictoException("No se puede eliminar la categoría porque tiene productos asociados");
        }
    }

    private Categoria buscarPorId(Long id) {
        return categoriaRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría con id " + id));
    }
}
