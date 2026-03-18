package com.example.wsevents.infrastructure.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registre thread-safe des sessions WebSocket actives.
 * Partagé entre le handler et l'adapter publisher.
 */
public class WebSocketSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionRegistry.class);

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("Session enregistrée — id={}, total={}", session.getId(), sessions.size());
    }

    public void unregister(String sessionId) {
        sessions.remove(sessionId);
        log.info("Session supprimée — id={}, total={}", sessionId, sessions.size());
    }

    public WebSocketSession get(String sessionId) {
        return sessions.get(sessionId);
    }

    public Collection<WebSocketSession> allSessions() {
        return sessions.values();
    }

    public int size() {
        return sessions.size();
    }
}
