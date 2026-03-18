package com.example.wsevents.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entité JPA — couche infra uniquement.
 * Le domaine ne connaît pas cette classe.
 */
@Entity
@Table(name = "event_message")
public class EventMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private String owner;

    protected EventMessageEntity() {}

    public EventMessageEntity(String payload, Instant createdAt, String owner) {
        this.payload   = payload;
        this.createdAt = createdAt;
        this.owner     = owner;
    }

    public Long    getId()        { return id; }
    public String  getPayload()   { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public String  getOwner()     { return owner; }
}
