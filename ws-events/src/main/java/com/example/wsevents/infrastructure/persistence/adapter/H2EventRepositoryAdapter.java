package com.example.wsevents.infrastructure.persistence.adapter;

import com.example.wsevents.domain.model.EventMessage;
import com.example.wsevents.domain.port.out.EventRepositoryPort;
import com.example.wsevents.infrastructure.persistence.entity.EventMessageEntity;
import com.example.wsevents.infrastructure.persistence.repository.EventMessageJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * Adapter sortant : implémente EventRepositoryPort via Spring Data JPA + H2.
 * Traduit les entités JPA en objets du domaine (mapping explicite, pas de mapper externe).
 */
public class H2EventRepositoryAdapter implements EventRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(H2EventRepositoryAdapter.class);

    private final EventMessageJpaRepository jpaRepository;

    public H2EventRepositoryAdapter(EventMessageJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public EventMessage save(String payload, String owner) {
        EventMessageEntity entity = new EventMessageEntity(payload, Instant.now(), owner);
        EventMessageEntity saved  = jpaRepository.save(entity);
        log.debug("Entité sauvegardée — id={}", saved.getId());
        return toDomain(saved);
    }

    @Override
    public List<EventMessage> findAfterId(Long lastId) {
        return jpaRepository
                .findByIdGreaterThanOrderByIdAsc(lastId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    // ── Mapping infra → domaine ──────────────────────────────────────────────

    private EventMessage toDomain(EventMessageEntity entity) {
        return new EventMessage(
                entity.getId(),
                entity.getPayload(),
                entity.getCreatedAt(),
                entity.getOwner()
        );
    }
}
