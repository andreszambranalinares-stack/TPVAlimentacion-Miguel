package com.tienda.tpv.dto;

import com.tienda.tpv.dominio.CierreCaja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Cierre de caja registrado. */
public record CierreCajaDTO(
        Long id,
        LocalDate fecha,
        int numeroVentas,
        BigDecimal totalVentas,
        BigDecimal totalEfectivo,
        BigDecimal totalTarjeta,
        BigDecimal efectivoContado,
        BigDecimal diferencia,
        String notas,
        LocalDateTime fechaHora,
        List<DenominacionDTO> denominaciones
) {

    public static CierreCajaDTO desde(CierreCaja c) {
        return new CierreCajaDTO(c.getId(), c.getFecha(), c.getNumeroVentas(), c.getTotalVentas(),
                c.getTotalEfectivo(), c.getTotalTarjeta(), c.getEfectivoContado(), c.getDiferencia(),
                c.getNotas(), c.getFechaHora(),
                c.getDenominaciones().stream().map(DenominacionDTO::desde).toList());
    }
}
