package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.ComponentePack;

import java.math.BigDecimal;

public record ComponentePackDTO(Long id, Long productoId, String productoNombre, BigDecimal cantidad) {

    public static ComponentePackDTO desde(ComponentePack c) {
        return new ComponentePackDTO(c.getId(), c.getComponente().getId(), c.getComponente().getNombre(), c.getCantidad());
    }
}
