package com.example.wsevents.infrastructure.config;

import com.example.wsevents.infrastructure.security.JwtHandshakeInterceptor;
import com.example.wsevents.infrastructure.security.JwtService;
import com.example.wsevents.infrastructure.websocket.EventWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Enregistrement du handler WebSocket natif avec son intercepteur JWT.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final EventWebSocketHandler   handler;
    private final JwtService              jwtService;

    public WebSocketConfig(EventWebSocketHandler handler, JwtService jwtService) {
        this.handler    = handler;
        this.jwtService = jwtService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            .addHandler(handler, "/ws/events")
            .addInterceptors(new JwtHandshakeInterceptor(jwtService))
            // Autoriser toutes les origines en dev — restreindre en prod
            .setAllowedOriginPatterns("*");
    }
}
