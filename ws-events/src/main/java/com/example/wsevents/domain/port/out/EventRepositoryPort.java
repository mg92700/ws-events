package com.example.wsevents.domain.port.out;

import com.example.wsevents.domain.model.EventMessage;

import java.util.List;

/**
 * Port de sortie vers la couche persistance.
 * Le domaine définit le contrat — l'infra l'implémente.
 */
public interface EventRepositoryPort {

    /**
     * Persiste un nouvel événement.
     *
     * @param payload contenu de l'événement
     * @param owner   utilisateur propriétaire
     * @return l'événement sauvegardé avec id et createdAt renseignés
     */
    EventMessage save(String payload, String owner);

    /**
     * Récupère tous les événements dont l'id est > lastId, triés par id ASC.
     */
    List<EventMessage> findAfterId(Long lastId);
}
