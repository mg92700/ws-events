package com.example.wsevents.domain.port.in;

import com.example.wsevents.domain.model.EventMessage;

import java.util.List;

/**
 * Port d'entrée : use case de reprise des événements manqués.
 * Appelé par le handler WebSocket lors d'une reconnexion.
 */
public interface ReplayEventPort {

    /**
     * Retourne tous les événements dont l'id est strictement supérieur à lastId.
     *
     * @param lastId dernier ID reçu par le client (0 = tout depuis le début)
     * @return liste ordonnée par ID croissant
     */
    List<EventMessage> replayAfter(Long lastId);
}
