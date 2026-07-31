package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.DenominacionCierre;

import java.math.BigDecimal;

public record DenominacionDTO(BigDecimal valor, Integer cantidad) {

    public static DenominacionDTO desde(DenominacionCierre d) {
        return new DenominacionDTO(d.getValor(), d.getCantidad());
    }
}
