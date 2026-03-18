package com.example.wsevents.entrypoint.rest.dto;

import java.util.List;

/**
 * Corps de la réponse POST /api/auth/token
 */
public record TokenResponse(String token, String username, List<String> roles, long expiresInMs) {}
