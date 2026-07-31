package com.tienda.tpv.controlador;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests de integración de proveedores (hueco de cobertura detectado en la revisión). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
@Transactional
class ProveedorApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PROVEEDOR_JSON = """
            { "nombre": "Distribuciones Norte", "telefono": "911222333", "contacto": "Ana" }
            """;

    @Test
    void crearProveedorDevuelve201YApareceEnElListado() throws Exception {
        mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROVEEDOR_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Distribuciones Norte"));

        mockMvc.perform(get("/api/proveedores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Distribuciones Norte"));
    }

    @Test
    void actualizarProveedorCambiaLosDatos() throws Exception {
        MvcResult creado = mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROVEEDOR_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(creado.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/proveedores/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nombre\": \"Distribuciones Sur\", \"telefono\": null, \"contacto\": null }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Distribuciones Sur"));
    }

    @Test
    void eliminarProveedorDevuelve204() throws Exception {
        MvcResult creado = mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROVEEDOR_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(creado.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/proveedores/" + id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/proveedores/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarUnProveedorConPedidosDevuelveConflicto() throws Exception {
        MvcResult proveedor = mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROVEEDOR_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        long idProveedor = objectMapper.readTree(proveedor.getResponse().getContentAsString()).get("id").asLong();

        MvcResult producto = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nombre": "Refrescos", "precioVenta": 1.50, "ivaPorcentaje": 10, "stockInicial": 0, "unidadMedida": "UNIDAD" }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long idProducto = objectMapper.readTree(producto.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/pedidos-proveedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "proveedorId": %d, "lineas": [ { "productoId": %d, "cantidad": 10 } ] }
                                """.formatted(idProveedor, idProducto)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/proveedores/" + idProveedor))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO"));

        mockMvc.perform(get("/api/proveedores/" + idProveedor))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerProveedorInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/proveedores/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void elCajeroNoPuedeCrearProveedores() throws Exception {
        mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROVEEDOR_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISOS"));
    }
}
