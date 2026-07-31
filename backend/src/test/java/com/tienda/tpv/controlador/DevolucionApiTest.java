package com.tienda.tpv.controlador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Casos de devolución no cubiertos ya en VentaApiTest: líneas ajenas y devolución de un pack. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
@Transactional
class DevolucionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long crearProducto(String nombre, String precio, String stockInicial) throws Exception {
        MvcResult creado = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nombre": "%s", "precioVenta": %s, "ivaPorcentaje": 10,
                                  "stockInicial": %s, "unidadMedida": "UNIDAD" }
                                """.formatted(nombre, precio, stockInicial)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(creado.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void devolverUnaLineaQueNoPerteneceALaVentaDevuelve400() throws Exception {
        long idProducto = crearProducto("Yogures pack 4", "2.00", "10");

        MvcResult venta1 = mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"metodoPago\": \"EFECTIVO\", \"lineas\": [ { \"productoId\": %d, \"cantidad\": 1 } ] }"
                                .formatted(idProducto)))
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult venta2 = mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"metodoPago\": \"EFECTIVO\", \"lineas\": [ { \"productoId\": %d, \"cantidad\": 1 } ] }"
                                .formatted(idProducto)))
                .andExpect(status().isCreated())
                .andReturn();

        long idVenta1 = objectMapper.readTree(venta1.getResponse().getContentAsString()).get("id").asLong();
        long idLineaVenta2 = objectMapper.readTree(venta2.getResponse().getContentAsString())
                .get("lineas").get(0).get("id").asLong();

        mockMvc.perform(post("/api/devoluciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "ventaId": %d, "lineas": [ { "lineaVentaId": %d, "cantidad": 1 } ] }
                                """.formatted(idVenta1, idLineaVenta2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void devolverUnaVentaDeUnPackReponeTambienSusComponentes() throws Exception {
        long idAgua = crearProducto("Agua 33cl", "0.40", "24");
        MvcResult pack = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Pack 6 aguas 33cl", "precioVenta": 2.00, "ivaPorcentaje": 10,
                                  "stockInicial": 3, "unidadMedida": "UNIDAD",
                                  "esPack": true, "componentes": [ { "productoId": %d, "cantidad": 6 } ]
                                }
                                """.formatted(idAgua)))
                .andExpect(status().isCreated())
                .andReturn();
        long idPack = objectMapper.readTree(pack.getResponse().getContentAsString()).get("id").asLong();

        MvcResult venta = mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"metodoPago\": \"EFECTIVO\", \"lineas\": [ { \"productoId\": %d, \"cantidad\": 1 } ] }"
                                .formatted(idPack)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode ventaJson = objectMapper.readTree(venta.getResponse().getContentAsString());
        long idVenta = ventaJson.get("id").asLong();
        long idLinea = ventaJson.get("lineas").get(0).get("id").asLong();

        mockMvc.perform(get("/api/productos/" + idAgua)).andExpect(jsonPath("$.stockActual").value(18)); // 24-6

        mockMvc.perform(post("/api/devoluciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "ventaId": %d, "lineas": [ { "lineaVentaId": %d, "cantidad": 1 } ] }
                                """.formatted(idVenta, idLinea)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/productos/" + idPack)).andExpect(jsonPath("$.stockActual").value(3)); // 3-1+1
        mockMvc.perform(get("/api/productos/" + idAgua)).andExpect(jsonPath("$.stockActual").value(24)); // 18+6
    }
}
