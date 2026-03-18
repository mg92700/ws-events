package com.example.wsevents.domain.model;

import java.util.List;

/**
 * Représente un utilisateur authentifié extrait du JWT.
 * Pas de dépendance Spring Security dans le domaine.
 */
public class AuthenticatedUser {

    private final String username;
    private final List<String> roles;

    public AuthenticatedUser(String username, List<String> roles) {
        this.username = username;
        this.roles    = roles != null ? List.copyOf(roles) : List.of();
    }

    public String getUsername()    { return username; }
    public List<String> getRoles() { return roles; }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    @Override
    public String toString() {
        return "AuthenticatedUser{username='" + username + "', roles=" + roles + "}";
    }
}
