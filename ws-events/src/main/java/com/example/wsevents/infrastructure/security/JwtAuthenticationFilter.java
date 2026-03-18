package com.example.wsevents.infrastructure.security;

import com.example.wsevents.domain.model.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filtre Spring Security : valide le JWT sur chaque requête HTTP.
 * Positionné avant UsernamePasswordAuthenticationFilter.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        extractToken(request).ifPresent(token -> {
            jwtService.validateAndExtract(token).ifPresent(user -> {
                var auth = buildAuthentication(user);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Authentification HTTP établie — user={}", user.getUsername());
            });
        });

        filterChain.doFilter(request, response);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }
        // Fallback : query param (utile pour les clients WebSocket qui ne gèrent pas les headers)
        String param = request.getParameter("access_token");
        if (param != null && !param.isBlank()) {
            return Optional.of(param);
        }
        return Optional.empty();
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(AuthenticatedUser user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        return new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                authorities
        );
    }
}
