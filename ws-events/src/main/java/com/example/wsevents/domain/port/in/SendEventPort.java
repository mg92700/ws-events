package com.example.wsevents.domain.port.in;

import com.example.wsevents.domain.model.EventMessage;

/**
 * Port d'entrée : use case d'envoi d'un événement.
 * Appelé par le controller REST.
 */
public interface SendEventPort {

    /**
     * Persiste le payload et le diffuse via WebSocket.
     *
     * @param payload contenu brut de l'événement
     * @param owner   utilisateur émetteur (extrait du JWT)
     * @return l'événement persisté avec son ID généré
     */
    EventMessage sendEvent(String payload, String owner);
}
