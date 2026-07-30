package com.tienda.tpv.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "codigo_barras", unique = true, length = 50)
    private String codigoBarras;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    /** Precio de venta al público, con IVA incluido. */
    @Column(name = "precio_venta", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "precio_coste", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioCoste = BigDecimal.ZERO;

    /** Valores permitidos: 0, 4, 10, 21. */
    @Column(name = "iva_porcentaje", nullable = false, precision = 4, scale = 1)
    private BigDecimal ivaPorcentaje;

    @Column(name = "stock_actual", nullable = false, precision = 10, scale = 3)
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Column(name = "stock_minimo", nullable = false, precision = 10, scale = 3)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_medida", nullable = false, length = 10)
    private UnidadMedida unidadMedida;

    @Column(nullable = false)
    private boolean activo = true;

    @Version
    @Column(nullable = false)
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public BigDecimal getPrecioCoste() {
        return precioCoste;
    }

    public void setPrecioCoste(BigDecimal precioCoste) {
        this.precioCoste = precioCoste;
    }

    public BigDecimal getIvaPorcentaje() {
        return ivaPorcentaje;
    }

    public void setIvaPorcentaje(BigDecimal ivaPorcentaje) {
        this.ivaPorcentaje = ivaPorcentaje;
    }

    public BigDecimal getStockActual() {
        return stockActual;
    }

    public void setStockActual(BigDecimal stockActual) {
        this.stockActual = stockActual;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(BigDecimal stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public UnidadMedida getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(UnidadMedida unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Long getVersion() {
        return version;
    }
}
