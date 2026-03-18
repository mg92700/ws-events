package com.example.wsevents.domain.port.out;

import com.example.wsevents.domain.model.EventMessage;

/**
 * Port de sortie vers le système de diffusion (WebSocket).
 * Le domaine reste agnostique du transport utilisé.
 */
public interface EventPublisherPort {

    /**
     * Diffuse un événement à tous les clients connectés.
     */
    void publish(EventMessage event);

    /**
     * Envoie un événement à une session WebSocket précise (utilisé pour le replay).
     *
     * @param sessionId identifiant de la session WebSocket cible
     * @param event     événement à envoyer
     */
    void publishToSession(String sessionId, EventMessage event);
}
