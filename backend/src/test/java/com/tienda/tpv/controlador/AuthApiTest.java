package com.tienda.tpv.controlador;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests del inicio de sesión con los usuarios sembrados por Flyway. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginCorrectoDevuelveElUsuarioYCreaSesion() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\": \"admin\", \"password\": \"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value("admin"))
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andReturn();

        MockHttpSession sesion = (MockHttpSession) resultado.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/yo").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value("admin"));
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\": \"admin\", \"password\": \"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void sinSesionLaApiDevuelve401() throws Exception {
        mockMvc.perform(get("/api/auth/yo")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    /**
     * El limitador de intentos es un bean singleton en memoria (no se revierte con
     * @Transactional): forzamos que Spring recree el contexto tras este test para
     * no dejar al usuario "caja" bloqueado de cara al resto de la suite.
     */
    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void trasVariosIntentosFallidosSeBloqueaElLoginAunConLaContrasenaCorrecta() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombreUsuario\": \"caja\", \"password\": \"mala\"}"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\": \"caja\", \"password\": \"caja123\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("DEMASIADOS_INTENTOS"));
    }
}
