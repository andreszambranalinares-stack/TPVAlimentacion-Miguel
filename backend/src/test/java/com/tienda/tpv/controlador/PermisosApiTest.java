package com.tienda.tpv.controlador;

import com.tienda.tpv.dominio.Producto;
import com.tienda.tpv.dominio.UnidadMedida;
import com.tienda.tpv.repositorio.ProductoRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El rol CAJERO puede cobrar y mover stock, pero no gestionar catálogo ni ver informes. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "CAJERO")
class PermisosApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductoRepositorio productoRepositorio;

    private Producto productoConStock() {
        Producto producto = new Producto();
        producto.setNombre("Chuches surtidas");
        producto.setPrecioVenta(new BigDecimal("1.00"));
        producto.setIvaPorcentaje(new BigDecimal("10"));
        producto.setStockActual(new BigDecimal("20"));
        producto.setUnidadMedida(UnidadMedida.UNIDAD);
        return productoRepositorio.save(producto);
    }

    @Test
    void elCajeroPuedeCobrarUnaVenta() throws Exception {
        Producto producto = productoConStock();
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "metodoPago": "EFECTIVO", "lineas": [ { "productoId": %d, "cantidad": 2 } ] }
                                """.formatted(producto.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    void elCajeroNoPuedeCrearProductos() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nombre": "Prohibido", "precioVenta": 1, "ivaPorcentaje": 21, "unidadMedida": "UNIDAD" }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISOS"));
    }

    @Test
    void elCajeroNoPuedeVerInformesNiCerrarCaja() throws Exception {
        mockMvc.perform(get("/api/informes/ventas")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/cierres-caja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"efectivoContado\": 100}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elCajeroSiPuedeConsultarProductos() throws Exception {
        productoConStock();
        mockMvc.perform(get("/api/productos").param("texto", "chuches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Chuches surtidas"));
    }
}
