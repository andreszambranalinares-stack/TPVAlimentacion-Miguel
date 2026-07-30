package com.tienda.tpv.controlador;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests de integración de movimientos manuales de stock. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
@Transactional
class MovimientoStockApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long productoId;

    @BeforeEach
    void crearProducto() throws Exception {
        MvcResult creado = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Lejía 1L",
                                  "precioVenta": 1.50,
                                  "ivaPorcentaje": 21,
                                  "stockInicial": 5,
                                  "unidadMedida": "UNIDAD"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        productoId = objectMapper.readTree(creado.getResponse().getContentAsString()).get("id").asLong();
    }

    private String movimientoJson(String tipo, String cantidad, String motivo) {
        return """
                { "productoId": %d, "tipo": "%s", "cantidad": %s, "motivo": "%s" }
                """.formatted(productoId, tipo, cantidad, motivo);
    }

    @Test
    void entradaDeCompraSumaStock() throws Exception {
        mockMvc.perform(post("/api/movimientos-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimientoJson("ENTRADA", "12", "Compra a proveedor Droguerías López")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidad").value(12));

        mockMvc.perform(get("/api/productos/" + productoId))
                .andExpect(jsonPath("$.stockActual").value(17));
    }

    @Test
    void mermaRestaStock() throws Exception {
        mockMvc.perform(post("/api/movimientos-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimientoJson("MERMA", "2", "Envases rotos")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidad").value(-2));

        mockMvc.perform(get("/api/productos/" + productoId))
                .andExpect(jsonPath("$.stockActual").value(3));
    }

    @Test
    void ajusteNegativoQueDejaStockNegativoDevuelve409() throws Exception {
        mockMvc.perform(post("/api/movimientos-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimientoJson("AJUSTE", "-8", "Recuento")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));

        mockMvc.perform(get("/api/productos/" + productoId))
                .andExpect(jsonPath("$.stockActual").value(5));
    }

    @Test
    void entradaConCantidadNegativaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/movimientos-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimientoJson("ENTRADA", "-3", "Error")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }
}
