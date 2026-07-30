package com.tienda.tpv.controlador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests de integración de la API de productos y categorías sobre H2. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PRODUCTO_JSON = """
            {
              "nombre": "Aceite de oliva 1L",
              "codigoBarras": "8410010000012",
              "precioVenta": 6.50,
              "precioCoste": 4.90,
              "ivaPorcentaje": 10,
              "stockMinimo": 4,
              "stockInicial": 10,
              "unidadMedida": "UNIDAD"
            }
            """;

    @Test
    void altaDeProductoYConsultaPorCodigoBarras() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.stockActual").value(10))
                .andExpect(jsonPath("$.bajoMinimo").value(false));

        mockMvc.perform(get("/api/productos/codigo-barras/8410010000012"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Aceite de oliva 1L"));
    }

    @Test
    void altaConCodigoBarrasDuplicadoDevuelve409() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO"));
    }

    @Test
    void altaSinNombreNiPrecioDevuelve400ConDetalles() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unidadMedida\": \"UNIDAD\", \"ivaPorcentaje\": 21}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles.nombre").exists())
                .andExpect(jsonPath("$.detalles.precioVenta").exists());
    }

    @Test
    void busquedaPorNombreParcialEncuentraElProducto() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_JSON))
                .andExpect(status().isCreated());

        MvcResult resultado = mockMvc.perform(get("/api/productos").param("texto", "aceite"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode lista = objectMapper.readTree(resultado.getResponse().getContentAsString());
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).get("nombre").asText()).isEqualTo("Aceite de oliva 1L");
    }

    @Test
    void borrarProductoEsBajaLogica() throws Exception {
        MvcResult creado = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(creado.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/productos/" + id))
                .andExpect(status().isNoContent());

        // Sigue existiendo pero inactivo, y ya no aparece en el listado por defecto
        mockMvc.perform(get("/api/productos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void crudDeCategorias() throws Exception {
        MvcResult creada = mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Droguería\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(creada.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"droguería\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/categorias/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    void laDocumentacionOpenApiEstaDisponible() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("TPV Alimentación — API"));
    }
}
