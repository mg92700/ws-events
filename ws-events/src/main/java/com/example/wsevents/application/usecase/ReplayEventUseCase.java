package com.example.wsevents.application.usecase;

import com.example.wsevents.domain.model.EventMessage;
import com.example.wsevents.domain.port.in.ReplayEventPort;
import com.example.wsevents.domain.port.out.EventPublisherPort;
import com.example.wsevents.domain.port.out.EventRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Use case de reprise (replay) à la reconnexion d'un client.
 */
public class ReplayEventUseCase implements ReplayEventPort {

    private static final Logger log = LoggerFactory.getLogger(ReplayEventUseCase.class);

    private final EventRepositoryPort repository;
    private final EventPublisherPort  publisher;

    public ReplayEventUseCase(EventRepositoryPort repository, EventPublisherPort publisher) {
        this.repository = repository;
        this.publisher  = publisher;
    }

    @Override
    public List<EventMessage> replayAfter(Long lastId) {
        long from = lastId != null ? lastId : 0L;
        log.debug("Replay demandé depuis id={}", from);

        List<EventMessage> events = repository.findAfterId(from);

        log.info("Replay : {} événement(s) trouvé(s) après id={}", events.size(), from);
        return events;
    }
}
