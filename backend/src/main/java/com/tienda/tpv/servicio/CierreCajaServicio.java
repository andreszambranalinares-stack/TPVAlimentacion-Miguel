package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.CierreCaja;
import com.tienda.tpv.dominio.DenominacionCierre;
import com.tienda.tpv.dto.CierreCajaDTO;
import com.tienda.tpv.dto.CierreCajaEntradaDTO;
import com.tienda.tpv.dto.DenominacionEntradaDTO;
import com.tienda.tpv.dto.InformeVentasDTO;
import com.tienda.tpv.excepciones.ConflictoException;
import com.tienda.tpv.excepciones.ValidacionException;
import com.tienda.tpv.repositorio.CierreCajaRepositorio;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class CierreCajaServicio {

    private final CierreCajaRepositorio cierreCajaRepositorio;
    private final InformeServicio informeServicio;

    public CierreCajaServicio(CierreCajaRepositorio cierreCajaRepositorio, InformeServicio informeServicio) {
        this.cierreCajaRepositorio = cierreCajaRepositorio;
        this.informeServicio = informeServicio;
    }

    /**
     * Cierra la caja de un día: congela las cifras de venta de esa fecha y
     * registra el arqueo (efectivo contado y diferencia frente a lo esperado).
     */
    public CierreCajaDTO cerrar(CierreCajaEntradaDTO entrada) {
        LocalDate fecha = entrada.fecha() != null ? entrada.fecha() : LocalDate.now();
        if (fecha.isAfter(LocalDate.now())) {
            throw new ValidacionException("No se puede cerrar la caja de una fecha futura");
        }
        if (cierreCajaRepositorio.existsByFecha(fecha)) {
            throw new ConflictoException("La caja del " + fecha + " ya está cerrada");
        }

        if (entrada.denominaciones() != null && !entrada.denominaciones().isEmpty()) {
            BigDecimal sumaDenominaciones = entrada.denominaciones().stream()
                    .map(d -> d.valor().multiply(BigDecimal.valueOf(d.cantidad())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sumaDenominaciones.setScale(2, java.math.RoundingMode.HALF_UP)
                    .compareTo(entrada.efectivoContado().setScale(2, java.math.RoundingMode.HALF_UP)) != 0) {
                throw new ValidacionException(
                        "El conteo por billetes y monedas (" + sumaDenominaciones
                                + " €) no coincide con el efectivo contado (" + entrada.efectivoContado() + " €)");
            }
        }

        InformeVentasDTO resumen = informeServicio.resumenVentas(fecha, fecha);
        CierreCaja cierre = new CierreCaja();
        cierre.setFecha(fecha);
        cierre.setNumeroVentas((int) resumen.numeroVentas());
        cierre.setTotalVentas(resumen.totalVentas());
        cierre.setTotalEfectivo(resumen.totalEfectivo());
        cierre.setTotalTarjeta(resumen.totalTarjeta());
        cierre.setEfectivoContado(entrada.efectivoContado());
        cierre.setDiferencia(entrada.efectivoContado().subtract(resumen.totalEfectivo()));
        cierre.setNotas(entrada.notas());
        if (entrada.denominaciones() != null) {
            for (DenominacionEntradaDTO d : entrada.denominaciones()) {
                if (d.cantidad() == 0) {
                    continue;
                }
                DenominacionCierre denominacion = new DenominacionCierre();
                denominacion.setValor(d.valor());
                denominacion.setCantidad(d.cantidad());
                cierre.agregarDenominacion(denominacion);
            }
        }
        return CierreCajaDTO.desde(cierreCajaRepositorio.save(cierre));
    }

    @Transactional(readOnly = true)
    public List<CierreCajaDTO> listar() {
        return cierreCajaRepositorio.findAll(Sort.by(Sort.Direction.DESC, "fecha")).stream()
                .map(CierreCajaDTO::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public CierreCajaDTO obtenerPorFecha(LocalDate fecha) {
        return cierreCajaRepositorio.findByFecha(fecha)
                .map(CierreCajaDTO::desde)
                .orElse(null);
    }
}
