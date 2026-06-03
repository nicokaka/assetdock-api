package com.assetdock.api.security;

import com.assetdock.api.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicEndpointsSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginEndpoint_shouldBeAccessibleWithoutAuth() throws Exception {
        // Se retornar 401: credenciais incorretas, o que significa que passou pelo Spring Security (Sucesso).
        // Se retornar 403: Spring Security bloqueou a rota indevidamente (Falha/Regressao).
        mockMvc.perform(post("/web/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"x@x.com\",\"password\":\"y\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void setupStatus_shouldReturnWithoutAuth() throws Exception {
        // Setup status deve ser publico e acessivel sem autenticacao
        mockMvc.perform(get("/setup/status"))
            .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_shouldReturnUnauthorizedWithoutAuth() throws Exception {
        // Rotas que nao estao explicitamente listadas como publicas devem retornar 401
        // Se retornar 200: falha de seguranca critica. Se retornar 403: errado tambem.
        mockMvc.perform(get("/users"))
            .andExpect(status().isUnauthorized());
    }
}
