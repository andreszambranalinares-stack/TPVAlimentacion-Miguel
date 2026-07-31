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

/** Un producto que forma parte de un pack, con la cantidad necesaria por cada unidad del pack. */
@Entity
@Table(name = "componente_pack")
public class ComponentePack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pack_id", nullable = false)
    private Producto pack;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "componente_id", nullable = false)
    private Producto componente;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal cantidad;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Producto getPack() {
        return pack;
    }

    public void setPack(Producto pack) {
        this.pack = pack;
    }

    public Producto getComponente() {
        return componente;
    }

    public void setComponente(Producto componente) {
        this.componente = componente;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }
}
