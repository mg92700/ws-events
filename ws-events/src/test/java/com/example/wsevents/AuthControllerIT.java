package com.example.wsevents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration — POST /api/auth/token
 *
 * Vérifie que le endpoint de génération de JWT répond correctement
 * pour les différents cas d'usage (user connu, admin, user par défaut).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── Cas nominaux ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/token avec user1 → retourne un JWT valide")
    void shouldReturnTokenForUser1() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "user1" }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.roles").isArray())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("token").asText()).contains(".");           // format JWT (3 segments)
        assertThat(body.get("expirationMs").asLong()).isPositive();
    }

    @Test
    @DisplayName("POST /api/auth/token avec admin → retourne roles USER + ADMIN")
    void shouldReturnAdminRoles() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "admin" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode roles = body.get("roles");
        assertThat(roles).isNotNull();
        assertThat(roles.toString()).contains("USER");
        assertThat(roles.toString()).contains("ADMIN");
    }

    @Test
    @DisplayName("POST /api/auth/token sans body → utilise user1 par défaut")
    void shouldUseDefaultUserWhenNoBody() throws Exception {
        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/auth/token avec body vide {} → utilise user1 par défaut")
    void shouldUseDefaultUserWhenEmptyBody() throws Exception {
        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"));
    }

    @Test
    @DisplayName("POST /api/auth/token avec user2 → retourne un token pour user2")
    void shouldReturnTokenForUser2() throws Exception {
        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "user2" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user2"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/auth/token avec username inconnu → retourne un token avec role USER par défaut")
    void shouldReturnDefaultRoleForUnknownUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "unknown_user" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("unknown_user"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("roles").toString()).contains("USER");
    }

    // ── Tokens distincts ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Deux appels successifs produisent des tokens différents (iat différent)")
    void shouldProduceDifferentTokensOnSuccessiveCalls() throws Exception {
        MvcResult r1 = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "user1" }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Thread.sleep(1000); // s'assure que iat diffère d'au moins 1 seconde

        MvcResult r2 = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "user1" }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String token1 = objectMapper.readTree(r1.getResponse().getContentAsString()).get("token").asText();
        String token2 = objectMapper.readTree(r2.getResponse().getContentAsString()).get("token").asText();

        assertThat(token1).isNotEqualTo(token2);
    }
}
