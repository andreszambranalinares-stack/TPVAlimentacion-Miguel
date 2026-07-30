package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.Categoria;
import com.tienda.tpv.dominio.MovimientoStock;
import com.tienda.tpv.dominio.Producto;
import com.tienda.tpv.dominio.TipoMovimiento;
import com.tienda.tpv.dto.ProductoDTO;
import com.tienda.tpv.dto.ProductoEntradaDTO;
import com.tienda.tpv.excepciones.ConflictoException;
import com.tienda.tpv.excepciones.RecursoNoEncontradoException;
import com.tienda.tpv.excepciones.ValidacionException;
import com.tienda.tpv.repositorio.CategoriaRepositorio;
import com.tienda.tpv.repositorio.MovimientoStockRepositorio;
import com.tienda.tpv.repositorio.ProductoRepositorio;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class ProductoServicio {

    private static final List<BigDecimal> IVAS_VALIDOS = List.of(
            new BigDecimal("0"), new BigDecimal("4"), new BigDecimal("10"), new BigDecimal("21"));

    private final ProductoRepositorio productoRepositorio;
    private final CategoriaRepositorio categoriaRepositorio;
    private final MovimientoStockRepositorio movimientoStockRepositorio;

    public ProductoServicio(ProductoRepositorio productoRepositorio,
                            CategoriaRepositorio categoriaRepositorio,
                            MovimientoStockRepositorio movimientoStockRepositorio) {
        this.productoRepositorio = productoRepositorio;
        this.categoriaRepositorio = categoriaRepositorio;
        this.movimientoStockRepositorio = movimientoStockRepositorio;
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> listar(String texto, Long categoriaId, boolean incluirInactivos) {
        List<Producto> productos;
        if (StringUtils.hasText(texto)) {
            productos = productoRepositorio.buscarActivos(texto.trim());
        } else {
            productos = productoRepositorio.findAll(Sort.by("nombre"));
        }
        return productos.stream()
                .filter(p -> incluirInactivos || p.isActivo())
                .filter(p -> categoriaId == null
                        || (p.getCategoria() != null && categoriaId.equals(p.getCategoria().getId())))
                .map(ProductoDTO::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoDTO obtener(Long id) {
        return ProductoDTO.desde(buscarPorId(id));
    }

    /** Búsqueda exacta para el lector de códigos de barras de la caja. */
    @Transactional(readOnly = true)
    public ProductoDTO obtenerPorCodigoBarras(String codigoBarras) {
        return productoRepositorio.findByCodigoBarras(codigoBarras)
                .filter(Producto::isActivo)
                .map(ProductoDTO::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe ningún producto activo con código de barras " + codigoBarras));
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> listarBajoMinimo() {
        return productoRepositorio.findBajoStockMinimo().stream()
                .map(ProductoDTO::desde)
                .toList();
    }

    public ProductoDTO crear(ProductoEntradaDTO entrada) {
        validarIva(entrada.ivaPorcentaje());
        String codigoBarras = normalizarCodigoBarras(entrada.codigoBarras());
        if (codigoBarras != null && productoRepositorio.existsByCodigoBarras(codigoBarras)) {
            throw new ConflictoException("Ya existe un producto con el código de barras " + codigoBarras);
        }

        Producto producto = new Producto();
        aplicarEntrada(producto, entrada, codigoBarras);

        BigDecimal stockInicial = entrada.stockInicial();
        if (stockInicial != null && stockInicial.signum() > 0) {
            producto.setStockActual(stockInicial);
        }
        producto = productoRepositorio.save(producto);

        if (stockInicial != null && stockInicial.signum() > 0) {
            MovimientoStock movimiento = new MovimientoStock();
            movimiento.setProducto(producto);
            movimiento.setTipo(TipoMovimiento.ENTRADA);
            movimiento.setCantidad(stockInicial);
            movimiento.setMotivo("Stock inicial en el alta del producto");
            movimientoStockRepositorio.save(movimiento);
        }
        return ProductoDTO.desde(producto);
    }

    public ProductoDTO actualizar(Long id, ProductoEntradaDTO entrada) {
        validarIva(entrada.ivaPorcentaje());
        Producto producto = buscarPorId(id);
        String codigoBarras = normalizarCodigoBarras(entrada.codigoBarras());
        if (codigoBarras != null) {
            productoRepositorio.findByCodigoBarras(codigoBarras)
                    .filter(otro -> !otro.getId().equals(id))
                    .ifPresent(otro -> {
                        throw new ConflictoException("Ya existe un producto con el código de barras " + codigoBarras);
                    });
        }
        // El stock actual no se toca aquí: solo cambia con ventas y movimientos.
        aplicarEntrada(producto, entrada, codigoBarras);
        return ProductoDTO.desde(producto);
    }

    /** Baja lógica: el producto deja de venderse pero conserva su histórico. */
    public void desactivar(Long id) {
        buscarPorId(id).setActivo(false);
    }

    private void aplicarEntrada(Producto producto, ProductoEntradaDTO entrada, String codigoBarras) {
        producto.setNombre(entrada.nombre().trim());
        producto.setCodigoBarras(codigoBarras);
        producto.setCategoria(buscarCategoria(entrada.categoriaId()));
        producto.setPrecioVenta(entrada.precioVenta());
        producto.setPrecioCoste(entrada.precioCoste() != null ? entrada.precioCoste() : BigDecimal.ZERO);
        producto.setIvaPorcentaje(entrada.ivaPorcentaje());
        producto.setStockMinimo(entrada.stockMinimo() != null ? entrada.stockMinimo() : BigDecimal.ZERO);
        producto.setUnidadMedida(entrada.unidadMedida());
        producto.setActivo(entrada.activo() == null || entrada.activo());
    }

    private Categoria buscarCategoria(Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        return categoriaRepositorio.findById(categoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría con id " + categoriaId));
    }

    private Producto buscarPorId(Long id) {
        return productoRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto con id " + id));
    }

    private void validarIva(BigDecimal iva) {
        if (iva != null && IVAS_VALIDOS.stream().noneMatch(valido -> valido.compareTo(iva) == 0)) {
            throw new ValidacionException("El IVA debe ser 0, 4, 10 o 21");
        }
    }

    private String normalizarCodigoBarras(String codigoBarras) {
        return StringUtils.hasText(codigoBarras) ? codigoBarras.trim() : null;
    }
}
