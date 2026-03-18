package com.example.wsevents.entrypoint.rest;

import com.example.wsevents.entrypoint.rest.dto.TokenResponse;
import com.example.wsevents.infrastructure.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Entrypoint REST : génération de JWT (mock users pour démonstration).
 *
 * POST /api/auth/token
 * Body : { "username": "user1" }   — optionnel, "user1" par défaut
 *
 * En production, on remplacerait le mock par une vérification en base.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /** Mock users définis statiquement pour la démo */
    private static final Map<String, List<String>> MOCK_USERS = Map.of(
            "user1", List.of("USER"),
            "user2", List.of("USER"),
            "admin", List.of("USER", "ADMIN")
    );

    private final JwtService jwtService;
    private final long       expirationMs;

    public AuthController(JwtService jwtService,
                          @Value("${jwt.expiration-ms}") long expirationMs) {
        this.jwtService   = jwtService;
        this.expirationMs = expirationMs;
    }

    /**
     * Génère un JWT pour l'utilisateur demandé.
     *
     * Exemple :
     * <pre>
     * POST /api/auth/token
     * { "username": "admin" }
     * </pre>
     */
    @PostMapping("/token")
    public ResponseEntity<TokenResponse> getToken(
            @RequestBody(required = false) Map<String, String> body) {

        String username = (body != null && body.containsKey("username"))
                ? body.get("username")
                : "user1";

        List<String> roles = MOCK_USERS.getOrDefault(username, List.of("USER"));

        String token = jwtService.generateToken(username, roles);

        log.info("Token émis pour user='{}', roles={}", username, roles);

        return ResponseEntity.ok(new TokenResponse(token, username, roles, expirationMs));
    }
}
