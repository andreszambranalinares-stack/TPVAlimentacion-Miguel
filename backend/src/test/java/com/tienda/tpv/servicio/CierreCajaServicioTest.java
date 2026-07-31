package com.tienda.tpv.servicio;

import com.tienda.tpv.dominio.CierreCaja;
import com.tienda.tpv.dto.CierreCajaDTO;
import com.tienda.tpv.dto.CierreCajaEntradaDTO;
import com.tienda.tpv.dto.InformeVentasDTO;
import com.tienda.tpv.excepciones.ValidacionException;
import com.tienda.tpv.repositorio.CierreCajaRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CierreCajaServicioTest {

    @Mock
    private CierreCajaRepositorio cierreCajaRepositorio;

    @Mock
    private InformeServicio informeServicio;

    @InjectMocks
    private CierreCajaServicio cierreCajaServicio;

    @Test
    void cerrarFechaFuturaLanzaValidacionSinTocarElRepositorio() {
        CierreCajaEntradaDTO entrada = new CierreCajaEntradaDTO(
                LocalDate.now().plusDays(1), new BigDecimal("10"), null, null);

        assertThatThrownBy(() -> cierreCajaServicio.cerrar(entrada))
                .isInstanceOf(ValidacionException.class);

        verify(cierreCajaRepositorio, never()).existsByFecha(any());
        verify(cierreCajaRepositorio, never()).save(any());
    }

    @Test
    void cerrarFechaPasadaSinVentasFunciona() {
        LocalDate fecha = LocalDate.now().minusDays(5);
        when(cierreCajaRepositorio.existsByFecha(fecha)).thenReturn(false);
        when(informeServicio.resumenVentas(fecha, fecha)).thenReturn(
                new InformeVentasDTO(fecha, fecha, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        when(cierreCajaRepositorio.save(any(CierreCaja.class))).thenAnswer(inv -> inv.getArgument(0));

        CierreCajaDTO cierre = cierreCajaServicio.cerrar(
                new CierreCajaEntradaDTO(fecha, new BigDecimal("0"), "Día sin ventas", null));

        assertThat(cierre.numeroVentas()).isZero();
        assertThat(cierre.diferencia()).isEqualByComparingTo("0");
    }
}
