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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration du flux complet :
 *  1. Obtenir un JWT via POST /api/auth/token
 *  2. Envoyer un événement via POST /api/events avec ce JWT
 *
 * Simule le comportement exact d'un client externe (Postman, curl, etc.).
 */
@SpringBootTest
@AutoConfigureMockMvc
class FullFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Flux complet : obtenir un token puis envoyer un événement")
    void shouldCompleteFullFlow() throws Exception {

        // ── Étape 1 : obtenir le JWT ─────────────────────────────────────────
        MvcResult tokenResponse = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "user1" }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tokenBody = objectMapper.readTree(tokenResponse.getResponse().getContentAsString());
        String token = tokenBody.get("token").asText();

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature

        // ── Étape 2 : envoyer un événement ───────────────────────────────────
        MvcResult eventResponse = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "payload": "evenement-flux-complet" }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode eventBody = objectMapper.readTree(eventResponse.getResponse().getContentAsString());

        assertThat(eventBody.get("id").asLong()).isPositive();
        assertThat(eventBody.get("payload").asText()).isEqualTo("evenement-flux-complet");
        assertThat(eventBody.get("owner").asText()).isEqualTo("user1");
        assertThat(eventBody.get("createdAt").asText()).isNotBlank();
    }

    @Test
    @DisplayName("Flux complet multi-utilisateurs : user1 et user2 ont des événements distincts")
    void shouldIsolateEventsByOwner() throws Exception {

        // Token user1
        String token1 = getToken("user1");

        // Token user2
        String token2 = getToken("user2");

        // user1 envoie un événement
        MvcResult r1 = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "payload": "event-de-user1" }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        // user2 envoie un événement
        MvcResult r2 = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "payload": "event-de-user2" }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode event1 = objectMapper.readTree(r1.getResponse().getContentAsString());
        JsonNode event2 = objectMapper.readTree(r2.getResponse().getContentAsString());

        // Vérifier les propriétaires
        assertThat(event1.get("owner").asText()).isEqualTo("user1");
        assertThat(event2.get("owner").asText()).isEqualTo("user2");

        // Vérifier les payloads
        assertThat(event1.get("payload").asText()).isEqualTo("event-de-user1");
        assertThat(event2.get("payload").asText()).isEqualTo("event-de-user2");

        // Vérifier que les IDs sont différents
        assertThat(event1.get("id").asLong()).isNotEqualTo(event2.get("id").asLong());
    }

    @Test
    @DisplayName("Le token expiré/invalide est rejeté même si la structure JWT est correcte")
    void shouldRejectTamperedToken() throws Exception {
        // Obtenir un vrai token puis altérer sa signature
        String validToken = getToken("user1");
        String tamperedToken = validToken.substring(0, validToken.lastIndexOf('.')) + ".signatureInvalide";

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + tamperedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "payload": "event-token-falsifie" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String getToken(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                { "username": "%s" }
                                """, username)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
