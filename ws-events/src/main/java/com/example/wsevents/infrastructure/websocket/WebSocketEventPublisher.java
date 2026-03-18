package com.example.wsevents.infrastructure.websocket;

import com.example.wsevents.domain.model.EventMessage;
import com.example.wsevents.domain.port.out.EventPublisherPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

/**
 * Adapter sortant : diffuse les EventMessage via WebSocket natif.
 * Implémente EventPublisherPort — le domaine n'a aucune connaissance du transport.
 */
public class WebSocketEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventPublisher.class);

    private final WebSocketSessionRegistry registry;
    private final ObjectMapper             objectMapper;

    public WebSocketEventPublisher(WebSocketSessionRegistry registry, ObjectMapper objectMapper) {
        this.registry     = registry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(EventMessage event) {
        String message = serialize(event);
        if (message == null) return;

        registry.allSessions().forEach(session -> sendSafely(session, message, event.getId()));
        log.debug("Broadcast id={} vers {} client(s)", event.getId(), registry.size());
    }

    @Override
    public void publishToSession(String sessionId, EventMessage event) {
        WebSocketSession session = registry.get(sessionId);
        if (session == null) {
            log.warn("Session introuvable pour le replay — sessionId={}", sessionId);
            return;
        }
        String message = serialize(event);
        if (message != null) {
            sendSafely(session, message, event.getId());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String serialize(EventMessage event) {
        try {
            Map<String, Object> payload = Map.of(
                    "id",        event.getId(),
                    "payload",   event.getPayload(),
                    "createdAt", event.getCreatedAt().toString(),
                    "owner",     event.getOwner()
            );
            return objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            log.error("Échec sérialisation événement id={}", event.getId(), e);
            return null;
        }
    }

    private void sendSafely(WebSocketSession session, String message, Long eventId) {
        if (!session.isOpen()) {
            log.warn("Session fermée, envoi ignoré — sessionId={}", session.getId());
            return;
        }
        try {
            synchronized (session) {   // WebSocketSession n'est pas thread-safe pour l'écriture
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            log.error("Erreur envoi WebSocket — sessionId={}, eventId={}", session.getId(), eventId, e);
        }
    }
}
