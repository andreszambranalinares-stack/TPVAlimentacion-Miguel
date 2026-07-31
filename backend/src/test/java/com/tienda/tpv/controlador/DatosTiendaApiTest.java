package com.tienda.tpv.controlador;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests de los datos fiscales/de contacto de la tienda, mostrados en el ticket. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DatosTiendaApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "CAJERO")
    void cualquierUsuarioAutenticadoPuedeLeerLosDatos() throws Exception {
        mockMvc.perform(get("/api/datos-tienda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Alimentación Miguel"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unAdminPuedeActualizarLosDatos() throws Exception {
        mockMvc.perform(put("/api/datos-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nombre": "Alimentación Miguel", "direccion": "Calle Mayor 3",
                                  "telefono": "911222333", "nif": "12345678A" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.direccion").value("Calle Mayor 3"))
                .andExpect(jsonPath("$.telefono").value("911222333"))
                .andExpect(jsonPath("$.nif").value("12345678A"));

        mockMvc.perform(get("/api/datos-tienda"))
                .andExpect(jsonPath("$.direccion").value("Calle Mayor 3"));
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void unCajeroNoPuedeActualizarLosDatos() throws Exception {
        mockMvc.perform(put("/api/datos-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nombre\": \"Otro nombre\" }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISOS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void elNombreEsObligatorio() throws Exception {
        mockMvc.perform(put("/api/datos-tienda")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nombre\": \"\" }"))
                .andExpect(status().isBadRequest());
    }
}
