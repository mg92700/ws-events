package com.example.wsevents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
 * Tests d'intégration — POST /api/events
 *
 * Couvre :
 *  - envoi d'un événement avec un JWT valide
 *  - refus sans token (401)
 *  - refus avec token invalide (401)
 *  - refus avec payload vide (400)
 */
@SpringBootTest
@AutoConfigureMockMvc
class EventControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** JWT obtenu avant chaque test */
    private String bearerToken;

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void obtainToken() throws Exception {
        MvcResult tokenResult = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "user1" }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(
                tokenResult.getResponse().getContentAsString()
        ).get("token").asText();

        bearerToken = "Bearer " + token;
    }

    // ── Cas nominaux ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/events avec JWT valide → 201 avec l'événement créé")
    void shouldCreateEventWithValidToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/events")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "payload": "mon-premier-evenement" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.payload").value("mon-premier-evenement"))
                .andExpect(jsonPath("$.owner").value("user1"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("id").asLong()).isPositive();
    }

    @Test
    @DisplayName("POST /api/events avec payload JSON complexe → 201")
    void shouldAcceptComplexJsonPayload() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "payload": "{\\"type\\":\\"ORDER_PLACED\\",\\"orderId\\":42}" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payload").value("{\"type\":\"ORDER_PLACED\",\"orderId\":42}"));
    }

    @Test
    @DisplayName("POST /api/events avec token admin → owner = admin")
    void shouldUseAdminAsOwner() throws Exception {
        // Obtenir un token admin
        MvcResult tokenResult = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "admin" }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String adminToken = "Bearer " + objectMapper.readTree(
                tokenResult.getResponse().getContentAsString()
        ).get("token").asText();

        mockMvc.perform(post("/api/events")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "payload": "evenement-admin" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner").value("admin"));
    }

    @Test
    @DisplayName("POST /api/events → chaque événement obtient un id auto-incrémenté distinct")
    void shouldIncrementIdOnEachEvent() throws Exception {
        MvcResult r1 = mockMvc.perform(post("/api/events")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "payload": "event-A" }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult r2 = mockMvc.perform(post("/api/events")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "payload": "event-B" }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        long id1 = objectMapper.readTree(r1.getResponse().getContentAsString()).get("id").asLong();
        long id2 = objectMapper.readTree(r2.getResponse().getContentAsString()).get("id").asLong();

        assertThat(id2).isGreaterThan(id1);
    }

    // ── Sécurité ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/events sans Authorization → 401")
    void shouldReturn401WhenNoToken() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "payload": "event-sans-token" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/events avec token malformé → 401")
    void shouldReturn401WhenMalformedToken() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer token.invalide.ici")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "payload": "event-token-invalide" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/events avec Authorization sans préfixe Bearer → 401")
    void shouldReturn401WhenNoBearerPrefix() throws Exception {
        // Extraire le token brut (sans "Bearer ")
        MvcResult tokenResult = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "user1" }
                                """))
                .andReturn();

        String rawToken = objectMapper.readTree(
                tokenResult.getResponse().getContentAsString()
        ).get("token").asText();

        mockMvc.perform(post("/api/events")
                        .header("Authorization", rawToken) // sans "Bearer "
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "payload": "event-sans-bearer" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── Validation du payload ─────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/events avec payload null → 400")
    void shouldReturn400WhenPayloadIsNull() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "payload": null }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/events avec payload vide → 400")
    void shouldReturn400WhenPayloadIsBlank() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "payload": "   " }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/events sans body → 400")
    void shouldReturn400WhenNoBody() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
