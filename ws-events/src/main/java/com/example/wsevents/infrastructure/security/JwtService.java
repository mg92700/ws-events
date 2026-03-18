package com.example.wsevents.infrastructure.security;

import com.example.wsevents.domain.model.AuthenticatedUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Service JWT autonome — HS256, pas de provider externe.
 *
 * Responsabilités :
 *  - génération de token (mock user ou user réel)
 *  - validation
 *  - extraction des claims vers AuthenticatedUser (domaine)
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final long      expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {

        this.key          = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    // ── Génération ───────────────────────────────────────────────────────────

    /**
     * Génère un JWT HS256 pour l'utilisateur donné.
     *
     * @param username identifiant
     * @param roles    liste des rôles (ex. ["USER", "ADMIN"])
     * @return token signé
     */
    public String generateToken(String username, List<String> roles) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        String token = Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();

        log.debug("Token généré pour user='{}', expiry={}", username, expiry);
        return token;
    }

    // ── Validation ───────────────────────────────────────────────────────────

    /**
     * Valide le token et retourne l'utilisateur extrait, ou empty si invalide/expiré.
     */
    public Optional<AuthenticatedUser> validateAndExtract(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String      username = claims.getSubject();
            List<String> roles   = extractRoles(claims);

            return Optional.of(new AuthenticatedUser(username, roles));

        } catch (ExpiredJwtException e) {
            log.warn("Token expiré : {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Token non supporté : {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Token malformé : {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("Signature JWT invalide : {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Token vide ou null : {}", e.getMessage());
        }
        return Optional.empty();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Claims claims) {
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }
}
