package com.example.wsevents.infrastructure.persistence.repository;

import com.example.wsevents.infrastructure.persistence.entity.EventMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository Spring Data JPA.
 * Limité à la couche infrastructure.
 */
public interface EventMessageJpaRepository extends JpaRepository<EventMessageEntity, Long> {

    /**
     * Retourne les événements dont l'id est strictement supérieur à lastId,
     * triés par id croissant.
     */
    List<EventMessageEntity> findByIdGreaterThanOrderByIdAsc(Long lastId);
}
