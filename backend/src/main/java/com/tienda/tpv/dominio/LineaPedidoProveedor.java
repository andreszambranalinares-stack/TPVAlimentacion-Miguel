package com.tienda.tpv.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "linea_pedido_proveedor")
public class LineaPedidoProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoProveedor pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "cantidad_pedida", nullable = false, precision = 10, scale = 3)
    private BigDecimal cantidadPedida;

    @Column(name = "cantidad_recibida", nullable = false, precision = 10, scale = 3)
    private BigDecimal cantidadRecibida = BigDecimal.ZERO;

    @Column(name = "precio_coste_unitario", precision = 10, scale = 2)
    private BigDecimal precioCosteUnitario;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PedidoProveedor getPedido() {
        return pedido;
    }

    public void setPedido(PedidoProveedor pedido) {
        this.pedido = pedido;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public BigDecimal getCantidadPedida() {
        return cantidadPedida;
    }

    public void setCantidadPedida(BigDecimal cantidadPedida) {
        this.cantidadPedida = cantidadPedida;
    }

    public BigDecimal getCantidadRecibida() {
        return cantidadRecibida;
    }

    public void setCantidadRecibida(BigDecimal cantidadRecibida) {
        this.cantidadRecibida = cantidadRecibida;
    }

    public BigDecimal getPrecioCosteUnitario() {
        return precioCosteUnitario;
    }

    public void setPrecioCosteUnitario(BigDecimal precioCosteUnitario) {
        this.precioCosteUnitario = precioCosteUnitario;
    }
}
