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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests de gestión de empleados: alta con acceso propio, guarda de administradores y cambio de contraseña. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
@Transactional
class UsuarioApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void unAdminPuedeDarDeAltaAUnEmpleadoConSuPropioAcceso() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombreUsuario": "lucia",
                                  "password": "claveSegura123",
                                  "nombre": "Lucía Fernández",
                                  "rol": "CAJERO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreUsuario").value("lucia"))
                .andExpect(jsonPath("$.activo").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\": \"lucia\", \"password\": \"claveSegura123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void unCajeroNoPuedeAccederALaGestionDeUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .with(user("cajero_test").roles("CAJERO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void noSePuedeDesactivarAlUnicoAdministradorActivo() throws Exception {
        long idAdmin = idDeUsuario("admin");

        mockMvc.perform(put("/api/usuarios/" + idAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nombre\": \"Administrador\", \"rol\": \"ADMIN\", \"activo\": false }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));

        // Con un segundo admin activo, ahora sí se puede desactivar al primero
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombreUsuario": "encargado2",
                                  "password": "otraClave123",
                                  "nombre": "Segundo Encargado",
                                  "rol": "ADMIN"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/usuarios/" + idAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nombre\": \"Administrador\", \"rol\": \"ADMIN\", \"activo\": false }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cambiarMiPasswordConLaActualCorrectaPermiteEntrarConLaNueva() throws Exception {
        mockMvc.perform(put("/api/usuarios/yo/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"passwordActual\": \"admin123\", \"passwordNueva\": \"nuevaClave456\" }"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\": \"admin\", \"password\": \"nuevaClave456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cambiarMiPasswordConLaActualIncorrectaDevuelve400() throws Exception {
        mockMvc.perform(put("/api/usuarios/yo/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"passwordActual\": \"mala\", \"passwordNueva\": \"nuevaClave456\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    private long idDeUsuario(String nombreUsuario) throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/usuarios")).andExpect(status().isOk()).andReturn();
        JsonNode lista = objectMapper.readTree(resultado.getResponse().getContentAsString());
        for (JsonNode usuario : lista) {
            if (usuario.get("nombreUsuario").asText().equals(nombreUsuario)) {
                return usuario.get("id").asLong();
            }
        }
        throw new IllegalStateException("No se encontró el usuario " + nombreUsuario);
    }
}
