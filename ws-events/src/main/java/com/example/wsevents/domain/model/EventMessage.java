package com.example.wsevents.domain.model;

import java.time.Instant;

/**
 * Aggregate racine du domaine.
 * Aucune dépendance framework — POJO pur.
 */
public class EventMessage {

    private final Long id;
    private final String payload;
    private final Instant createdAt;
    private final String owner;   // Bonus : filtrage par user

    public EventMessage(Long id, String payload, Instant createdAt, String owner) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Le payload ne peut pas être vide");
        }
        this.id        = id;
        this.payload   = payload;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.owner     = owner;
    }

    public Long getId()          { return id; }
    public String getPayload()   { return payload; }
    public Instant getCreatedAt(){ return createdAt; }
    public String getOwner()     { return owner; }

    @Override
    public String toString() {
        return "EventMessage{id=" + id + ", owner='" + owner + "', createdAt=" + createdAt + "}";
    }
}
