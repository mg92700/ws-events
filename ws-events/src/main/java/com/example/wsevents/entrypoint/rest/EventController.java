package com.example.wsevents.entrypoint.rest;

import com.example.wsevents.domain.model.EventMessage;
import com.example.wsevents.domain.port.in.SendEventPort;
import com.example.wsevents.entrypoint.rest.dto.SendEventRequest;
import com.example.wsevents.entrypoint.rest.dto.SendEventResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Entrypoint REST : réception des événements à publier.
 *
 * POST /api/events
 * Header : Authorization: Bearer <jwt>
 * Body   : { "payload": "..." }
 *
 * Le controller extrait le username depuis le SecurityContext (déjà peuplé
 * par JwtAuthenticationFilter) puis délègue au use case SendEventPort.
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final SendEventPort sendEventPort;

    public EventController(SendEventPort sendEventPort) {
        this.sendEventPort = sendEventPort;
    }

    @PostMapping
    public ResponseEntity<SendEventResponse> sendEvent(
            @RequestBody SendEventRequest request,
            Authentication authentication) {

        if (request == null || request.payload() == null || request.payload().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String owner = authentication != null ? authentication.getName() : "anonymous";
        log.debug("Requête POST /api/events — owner={}", owner);

        EventMessage saved = sendEventPort.sendEvent(request.payload(), owner);

        SendEventResponse response = new SendEventResponse(
                saved.getId(),
                saved.getPayload(),
                saved.getCreatedAt(),
                saved.getOwner()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
