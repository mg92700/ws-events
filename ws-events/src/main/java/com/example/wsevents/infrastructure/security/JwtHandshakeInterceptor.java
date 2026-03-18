package com.example.wsevents.infrastructure.security;

import com.example.wsevents.domain.model.AuthenticatedUser;
import com.example.wsevents.infrastructure.websocket.EventWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

/**
 * Intercepteur de handshake WebSocket.
 *
 * Stratégie d'extraction du token (priorité décroissante) :
 *  1. Header HTTP  : Authorization: Bearer <token>
 *  2. Query param  : ?access_token=<token>
 *
 * Refuse la connexion (retourne false) si le JWT est absent ou invalide.
 * Stocke username + roles dans les attributs de session pour le handler.
 */
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    private final JwtService jwtService;

    public JwtHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        Optional<String> tokenOpt = extractToken(request);

        if (tokenOpt.isEmpty()) {
            log.warn("Handshake refusé — aucun token JWT fourni (uri={})", request.getURI());
            return false;
        }

        Optional<AuthenticatedUser> userOpt = jwtService.validateAndExtract(tokenOpt.get());

        if (userOpt.isEmpty()) {
            log.warn("Handshake refusé — JWT invalide (uri={})", request.getURI());
            return false;
        }

        AuthenticatedUser user = userOpt.get();
        attributes.put(EventWebSocketHandler.ATTR_USERNAME, user.getUsername());
        attributes.put(EventWebSocketHandler.ATTR_ROLES,    user.getRoles());

        log.info("Handshake accepté — user={}, roles={}", user.getUsername(), user.getRoles());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // Rien à faire après le handshake
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Optional<String> extractToken(ServerHttpRequest request) {
        // 1. Header Authorization
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return Optional.of(authHeader.substring(7).trim());
        }

        // 2. Query param ?access_token=...
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String param = servletRequest.getServletRequest().getParameter("access_token");
            if (param != null && !param.isBlank()) {
                return Optional.of(param.trim());
            }
        }

        return Optional.empty();
    }
}
