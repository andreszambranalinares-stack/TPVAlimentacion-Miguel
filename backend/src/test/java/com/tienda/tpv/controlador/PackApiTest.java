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

/** Tests de packs: al vender el pack se descuenta también el stock de sus componentes. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
@Transactional
class PackApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long idAgua;

    @BeforeEach
    void crearComponente() throws Exception {
        MvcResult creado = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Agua mineral 50cl",
                                  "precioVenta": 0.50,
                                  "ivaPorcentaje": 10,
                                  "stockInicial": 24,
                                  "unidadMedida": "UNIDAD"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        idAgua = objectMapper.readTree(creado.getResponse().getContentAsString()).get("id").asLong();
    }

    private long crearPackDe6Aguas() throws Exception {
        MvcResult creado = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Pack 6 aguas 50cl",
                                  "precioVenta": 2.50,
                                  "ivaPorcentaje": 10,
                                  "stockInicial": 3,
                                  "unidadMedida": "UNIDAD",
                                  "esPack": true,
                                  "componentes": [ { "productoId": %d, "cantidad": 6 } ]
                                }
                                """.formatted(idAgua)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.esPack").value(true))
                .andExpect(jsonPath("$.componentes[0].productoNombre").value("Agua mineral 50cl"))
                .andReturn();
        return objectMapper.readTree(creado.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void venderUnPackDescuentaSuStockYElDeSusComponentes() throws Exception {
        long idPack = crearPackDe6Aguas();

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "metodoPago": "EFECTIVO", "lineas": [ { "productoId": %d, "cantidad": 1 } ] }
                                """.formatted(idPack)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/productos/" + idPack))
                .andExpect(jsonPath("$.stockActual").value(2)); // 3 - 1
        mockMvc.perform(get("/api/productos/" + idAgua))
                .andExpect(jsonPath("$.stockActual").value(18)); // 24 - 6

        MvcResult movimientos = mockMvc.perform(get("/api/movimientos-stock")
                        .param("productoId", String.valueOf(idAgua)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode salida = objectMapper.readTree(movimientos.getResponse().getContentAsString()).get(0);
        assertThat(salida.get("cantidad").decimalValue()).isEqualByComparingTo("-6");
    }

    @Test
    void venderUnPackSinStockSuficienteDeUnComponenteDevuelve409() throws Exception {
        // Solo hay 24 aguas; vender 5 packs (30 aguas) debe fallar
        long idPack = crearPackDe6Aguas();
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "metodoPago": "EFECTIVO", "lineas": [ { "productoId": %d, "cantidad": 5 } ] }
                                """.formatted(idPack)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));

        // Nada se ha descontado: la transacción se revierte entera
        mockMvc.perform(get("/api/productos/" + idAgua))
                .andExpect(jsonPath("$.stockActual").value(24));
    }

    @Test
    void unPackNoPuedeTenerOtroPackComoComponente() throws Exception {
        long idPack = crearPackDe6Aguas();
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Pack doble",
                                  "precioVenta": 5.00,
                                  "ivaPorcentaje": 10,
                                  "unidadMedida": "UNIDAD",
                                  "esPack": true,
                                  "componentes": [ { "productoId": %d, "cantidad": 2 } ]
                                }
                                """.formatted(idPack)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }
}
