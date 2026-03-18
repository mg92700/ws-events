package com.example.wsevents.infrastructure.websocket;

import com.example.wsevents.domain.model.EventMessage;
import com.example.wsevents.domain.port.in.ReplayEventPort;
import com.example.wsevents.domain.port.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;

/**
 * Handler WebSocket natif (sans STOMP).
 *
 * À la connexion :
 *  1. Enregistre la session
 *  2. Lit le query param ?lastId=XX
 *  3. Rejoue les messages manqués
 *
 * Le token a déjà été validé par JwtHandshakeInterceptor.
 */
public class EventWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(EventWebSocketHandler.class);

    /** Clé stockée dans les attributs de session par le HandshakeInterceptor */
    public static final String ATTR_USERNAME = "username";
    public static final String ATTR_ROLES    = "roles";

    private final WebSocketSessionRegistry registry;
    private final ReplayEventPort          replayPort;
    private final EventPublisherPort       publisher;

    public EventWebSocketHandler(WebSocketSessionRegistry registry,
                                 ReplayEventPort replayPort,
                                 EventPublisherPort publisher) {
        this.registry   = registry;
        this.replayPort = replayPort;
        this.publisher  = publisher;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = (String) session.getAttributes().get(ATTR_USERNAME);
        log.info("WebSocket connecté — sessionId={}, user={}", session.getId(), username);

        registry.register(session);

        // Replay des messages manqués
        Long lastId = extractLastId(session);
        if (lastId != null) {
            log.debug("Replay demandé depuis lastId={} pour sessionId={}", lastId, session.getId());
            List<EventMessage> missed = replayPort.replayAfter(lastId);
            missed.forEach(event -> publisher.publishToSession(session.getId(), event));
            log.info("Replay terminé : {} message(s) envoyé(s) à sessionId={}", missed.size(), session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Dans ce projet le client est read-only (abonné uniquement).
        // On log mais on n'agit pas — extensible pour les ACK futurs.
        log.debug("Message reçu du client sessionId={} : {}", session.getId(), message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket déconnecté — sessionId={}, status={}", session.getId(), status);
        registry.unregister(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Erreur transport WebSocket — sessionId={}", session.getId(), exception);
        registry.unregister(session.getId());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Long extractLastId(WebSocketSession session) {
        String uri = session.getUri() != null ? session.getUri().toString() : "";
        Map<String, String> params = parseQueryParams(uri);
        String raw = params.get("lastId");
        if (raw == null || raw.isBlank()) return null;
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            log.warn("Paramètre lastId invalide : '{}'", raw);
            return null;
        }
    }

    private Map<String, String> parseQueryParams(String uri) {
        int idx = uri.indexOf('?');
        if (idx < 0) return Map.of();

        String query = uri.substring(idx + 1);
        Map<String, String> result = new java.util.HashMap<>();
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) result.put(kv[0], kv[1]);
        }
        return result;
    }
}
