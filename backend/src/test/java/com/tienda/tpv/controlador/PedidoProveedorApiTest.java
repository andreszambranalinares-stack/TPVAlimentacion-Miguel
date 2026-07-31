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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests de pedidos formales a proveedor: creación (ADMIN) y recepción parcial/total (cualquiera). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
@Transactional
class PedidoProveedorApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long idProducto;
    private long idProveedor;

    @BeforeEach
    void crearProductoYProveedor() throws Exception {
        MvcResult producto = mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Latas de conserva",
                                  "precioVenta": 2.00,
                                  "ivaPorcentaje": 10,
                                  "stockInicial": 5,
                                  "unidadMedida": "UNIDAD"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        idProducto = objectMapper.readTree(producto.getResponse().getContentAsString()).get("id").asLong();

        MvcResult proveedor = mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nombre\": \"Conservas del Norte\" }"))
                .andExpect(status().isCreated())
                .andReturn();
        idProveedor = objectMapper.readTree(proveedor.getResponse().getContentAsString()).get("id").asLong();
    }

    private long crearPedidoDe20() throws Exception {
        MvcResult creado = mockMvc.perform(post("/api/pedidos-proveedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "proveedorId": %d,
                                  "lineas": [ { "productoId": %d, "cantidad": 20, "precioCosteUnitario": 1.10 } ]
                                }
                                """.formatted(idProveedor, idProducto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn();
        return objectMapper.readTree(creado.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void unCajeroNoPuedeCrearUnPedido() throws Exception {
        mockMvc.perform(post("/api/pedidos-proveedor")
                        .with(user("cajero_test").roles("CAJERO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "proveedorId": %d, "lineas": [ { "productoId": %d, "cantidad": 10 } ] }
                                """.formatted(idProveedor, idProducto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void recibirParcialmenteDejaElPedidoEnRecibidoParcialYSumaStock() throws Exception {
        long idPedido = crearPedidoDe20();

        // Un cajero puede recibir la mercancía aunque no pueda crear el pedido
        mockMvc.perform(post("/api/pedidos-proveedor/" + idPedido + "/recibir")
                        .with(user("cajero_test").roles("CAJERO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "lineas": [ { "lineaPedidoId": %d, "cantidadRecibida": 12 } ] }
                                """.formatted(obtenerPrimeraLineaId(idPedido))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECIBIDO_PARCIAL"))
                .andExpect(jsonPath("$.lineas[0].cantidadPendiente").value(8));

        mockMvc.perform(get("/api/productos/" + idProducto))
                .andExpect(jsonPath("$.stockActual").value(17)); // 5 + 12
    }

    @Test
    void recibirElRestoCompletaElPedido() throws Exception {
        long idPedido = crearPedidoDe20();
        long idLinea = obtenerPrimeraLineaId(idPedido);

        mockMvc.perform(post("/api/pedidos-proveedor/" + idPedido + "/recibir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"lineas\": [ { \"lineaPedidoId\": %d, \"cantidadRecibida\": 20 } ] }"
                                .formatted(idLinea)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECIBIDO_COMPLETO"));

        // Ya no admite más recepciones
        mockMvc.perform(post("/api/pedidos-proveedor/" + idPedido + "/recibir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"lineas\": [ { \"lineaPedidoId\": %d, \"cantidadRecibida\": 1 } ] }"
                                .formatted(idLinea)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelarUnPedidoYaRecibidoDaConflicto() throws Exception {
        long idPedido = crearPedidoDe20();
        long idLinea = obtenerPrimeraLineaId(idPedido);
        mockMvc.perform(post("/api/pedidos-proveedor/" + idPedido + "/recibir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"lineas\": [ { \"lineaPedidoId\": %d, \"cantidadRecibida\": 1 } ] }"
                                .formatted(idLinea)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/pedidos-proveedor/" + idPedido + "/cancelar"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelarUnPedidoSinRecibirFunciona() throws Exception {
        long idPedido = crearPedidoDe20();
        mockMvc.perform(post("/api/pedidos-proveedor/" + idPedido + "/cancelar"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/pedidos-proveedor/" + idPedido))
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
    }

    private long obtenerPrimeraLineaId(long idPedido) throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/pedidos-proveedor/" + idPedido))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode pedido = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return pedido.get("lineas").get(0).get("id").asLong();
    }
}
