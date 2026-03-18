package com.example.wsevents.application.usecase;

import com.example.wsevents.domain.model.EventMessage;
import com.example.wsevents.domain.port.in.SendEventPort;
import com.example.wsevents.domain.port.out.EventPublisherPort;
import com.example.wsevents.domain.port.out.EventRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestration : persiste l'événement puis le publie via WebSocket.
 *
 * Injection par constructeur uniquement — pas d'annotation Spring dans le domaine.
 * La classe est annotée dans la couche infra/config.
 */
public class SendEventUseCase implements SendEventPort {

    private static final Logger log = LoggerFactory.getLogger(SendEventUseCase.class);

    private final EventRepositoryPort repository;
    private final EventPublisherPort  publisher;

    public SendEventUseCase(EventRepositoryPort repository, EventPublisherPort publisher) {
        this.repository = repository;
        this.publisher  = publisher;
    }

    @Override
    public EventMessage sendEvent(String payload, String owner) {
        log.debug("Envoi événement — owner={}, payload='{}'", owner, payload);

        EventMessage saved = repository.save(payload, owner);

        log.info("Événement persisté — id={}, owner={}", saved.getId(), saved.getOwner());

        publisher.publish(saved);

        log.debug("Événement diffusé via WebSocket — id={}", saved.getId());

        return saved;
    }
}
