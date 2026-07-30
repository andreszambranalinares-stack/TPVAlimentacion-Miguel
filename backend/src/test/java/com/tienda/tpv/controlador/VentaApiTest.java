package com.tienda.tpv.controlador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests de integración de la lógica de ventas: stock, movimientos e IVA. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
@Transactional
class VentaApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long productoId;

    @BeforeEach
    void crearProductoConStock() throws Exception {
        // Refresco 33cl: 1,21 € con 21% de IVA y 10 unidades de stock
        MvcResult creado = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Refresco de cola 33cl",
                                  "codigoBarras": "5449000000996",
                                  "precioVenta": 1.21,
                                  "ivaPorcentaje": 21,
                                  "stockInicial": 10,
                                  "unidadMedida": "UNIDAD"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        productoId = objectMapper.readTree(creado.getResponse().getContentAsString()).get("id").asLong();
    }

    private String ventaJson(long productoId, String cantidad) {
        return """
                {
                  "metodoPago": "EFECTIVO",
                  "lineas": [ { "productoId": %d, "cantidad": %s } ]
                }
                """.formatted(productoId, cantidad);
    }

    @Test
    void crearVentaDescuentaStockCalculaIvaYGeneraMovimiento() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ventaJson(productoId, "2")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(2.42))
                .andExpect(jsonPath("$.totalIva").value(0.42))
                .andExpect(jsonPath("$.lineas[0].subtotal").value(2.42));

        // El stock queda descontado
        mockMvc.perform(get("/api/productos/" + productoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockActual").value(8));

        // Y hay un movimiento SALIDA de -2 con el nº de venta en el motivo
        MvcResult movimientos = mockMvc.perform(get("/api/movimientos-stock")
                        .param("productoId", String.valueOf(productoId)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode lista = objectMapper.readTree(movimientos.getResponse().getContentAsString());
        JsonNode salida = lista.get(0);
        assertThat(salida.get("tipo").asText()).isEqualTo("SALIDA");
        assertThat(salida.get("cantidad").decimalValue()).isEqualByComparingTo("-2");
        assertThat(salida.get("motivo").asText()).startsWith("Venta #");
    }

    @Test
    void ventaConStockInsuficienteDevuelve409YNoDescuenta() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ventaJson(productoId, "11")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));

        mockMvc.perform(get("/api/productos/" + productoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockActual").value(10));
    }

    @Test
    void ventaSinLineasDevuelve400() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\": \"TARJETA\", \"lineas\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void ventaDeProductoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ventaJson(99999L, "1")))
                .andExpect(status().isNotFound());
    }

    @Test
    void laVentaApareceEnElListadoDeHoyYEnElInforme() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ventaJson(productoId, "3")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/ventas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].total").value(3.63));

        mockMvc.perform(get("/api/informes/ventas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroVentas").value(1))
                .andExpect(jsonPath("$.totalVentas").value(3.63))
                .andExpect(jsonPath("$.totalEfectivo").value(3.63))
                .andExpect(jsonPath("$.totalTarjeta").value(0));

        mockMvc.perform(get("/api/informes/productos-mas-vendidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Refresco de cola 33cl"))
                .andExpect(jsonPath("$[0].cantidadVendida").value(3));
    }
}
