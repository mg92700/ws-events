package com.example.wsevents.entrypoint.rest.dto;

import java.time.Instant;

/**
 * Corps de la réponse POST /api/events
 */
public record SendEventResponse(Long id, String payload, Instant createdAt, String owner) {}
