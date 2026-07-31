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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests del cierre de caja diario y de la exportación CSV. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "ADMIN")
class CierreCajaApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void venderAlgoHoy() throws Exception {
        MvcResult creado = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Barra de pan",
                                  "precioVenta": 1.00,
                                  "ivaPorcentaje": 4,
                                  "stockInicial": 30,
                                  "unidadMedida": "UNIDAD"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long productoId = objectMapper.readTree(creado.getResponse().getContentAsString()).get("id").asLong();

        // Dos ventas de hoy: 3 € en efectivo y 2 € con tarjeta
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\": \"EFECTIVO\", \"lineas\": [{\"productoId\": %d, \"cantidad\": 3}]}"
                                .formatted(productoId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodoPago\": \"TARJETA\", \"lineas\": [{\"productoId\": %d, \"cantidad\": 2}]}"
                                .formatted(productoId)))
                .andExpect(status().isCreated());
    }

    @Test
    void cerrarLaCajaCalculaLaDiferenciaDelArqueo() throws Exception {
        // Esperados 3 € en efectivo; contados 2,50 € → faltan 0,50 €
        mockMvc.perform(post("/api/cierres-caja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"efectivoContado\": 2.50, \"notas\": \"Falta suelto\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroVentas").value(2))
                .andExpect(jsonPath("$.totalVentas").value(5.0))
                .andExpect(jsonPath("$.totalEfectivo").value(3.0))
                .andExpect(jsonPath("$.totalTarjeta").value(2.0))
                .andExpect(jsonPath("$.diferencia").value(-0.5));

        mockMvc.perform(get("/api/cierres-caja"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notas").value("Falta suelto"));
    }

    @Test
    void noSePuedeCerrarDosVecesElMismoDia() throws Exception {
        mockMvc.perform(post("/api/cierres-caja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"efectivoContado\": 3.00}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cierres-caja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"efectivoContado\": 3.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO"));
    }

    @Test
    void elCsvDeVentasIncluyeLasVentasDeHoy() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/informes/ventas.csv"))
                .andExpect(status().isOk())
                .andExpect(status().isOk())
                .andReturn();

        String contenido = resultado.getResponse().getContentAsString();
        assertThat(resultado.getResponse().getContentType()).contains("text/csv");
        assertThat(contenido).contains("Ticket;Fecha;Hora;Método de pago;Total;IVA incluido");
        assertThat(contenido).contains("EFECTIVO;3,00");
        assertThat(contenido).contains("TARJETA;2,00");
    }

    @Test
    void elCsvDeInventarioIncluyeElProducto() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/informes/inventario.csv"))
                .andExpect(status().isOk())
                .andReturn();

        // 30 iniciales - 5 vendidas = 25 en stock
        assertThat(resultado.getResponse().getContentAsString())
                .contains("Barra de pan")
                .contains(";25;");
    }

    @Test
    void elConteoPorDenominacionQueCoincideConElTotalSeAceptaYSeGuarda() throws Exception {
        // 1 billete de 2 € + 1 moneda de 1 € = 3,00 €, coincide con lo esperado en efectivo
        mockMvc.perform(post("/api/cierres-caja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "efectivoContado": 3.00,
                                  "denominaciones": [
                                    { "valor": 2, "cantidad": 1 },
                                    { "valor": 1, "cantidad": 1 }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.denominaciones", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    void elConteoPorDenominacionQueNoCoincideDevuelve400() throws Exception {
        // 1 moneda de 1 € = 1,00 €, pero dice que ha contado 3,00 €
        mockMvc.perform(post("/api/cierres-caja")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "efectivoContado": 3.00,
                                  "denominaciones": [ { "valor": 1, "cantidad": 1 } ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }
}
