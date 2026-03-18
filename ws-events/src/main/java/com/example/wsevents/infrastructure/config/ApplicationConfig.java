package com.example.wsevents.infrastructure.config;

import com.example.wsevents.application.usecase.ReplayEventUseCase;
import com.example.wsevents.application.usecase.SendEventUseCase;
import com.example.wsevents.domain.port.in.ReplayEventPort;
import com.example.wsevents.domain.port.in.SendEventPort;
import com.example.wsevents.domain.port.out.EventPublisherPort;
import com.example.wsevents.domain.port.out.EventRepositoryPort;
import com.example.wsevents.infrastructure.persistence.adapter.H2EventRepositoryAdapter;
import com.example.wsevents.infrastructure.persistence.repository.EventMessageJpaRepository;
import com.example.wsevents.infrastructure.websocket.EventWebSocketHandler;
import com.example.wsevents.infrastructure.websocket.WebSocketEventPublisher;
import com.example.wsevents.infrastructure.websocket.WebSocketSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration centrale : câblage des ports et adapters.
 *
 * Ce fichier est le seul endroit où Spring connaît les implémentations concrètes.
 * Les use cases ne voient que des interfaces (ports).
 */
@Configuration
public class ApplicationConfig {

    // ── Infrastructure Beans ─────────────────────────────────────────────────

    @Bean
    public WebSocketSessionRegistry webSocketSessionRegistry() {
        return new WebSocketSessionRegistry();
    }

    @Bean
    public EventPublisherPort eventPublisherPort(WebSocketSessionRegistry registry,
                                                  ObjectMapper objectMapper) {
        return new WebSocketEventPublisher(registry, objectMapper);
    }

    @Bean
    public EventRepositoryPort eventRepositoryPort(EventMessageJpaRepository jpaRepository) {
        return new H2EventRepositoryAdapter(jpaRepository);
    }

    // ── Use Cases (Application Layer) ────────────────────────────────────────

    @Bean
    public SendEventPort sendEventPort(EventRepositoryPort repository,
                                       EventPublisherPort publisher) {
        return new SendEventUseCase(repository, publisher);
    }

    @Bean
    public ReplayEventPort replayEventPort(EventRepositoryPort repository,
                                            EventPublisherPort publisher) {
        return new ReplayEventUseCase(repository, publisher);
    }

    // ── WebSocket Handler ────────────────────────────────────────────────────

    @Bean
    public EventWebSocketHandler eventWebSocketHandler(WebSocketSessionRegistry registry,
                                                        ReplayEventPort replayPort,
                                                        EventPublisherPort publisher) {
        return new EventWebSocketHandler(registry, replayPort, publisher);
    }
}
