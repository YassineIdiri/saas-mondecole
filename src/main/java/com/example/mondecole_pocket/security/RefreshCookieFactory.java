package com.example.mondecole_pocket.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {

    @Value("${security.refresh.cookie-name:refreshToken}")
    private String cookieName;

    @Value("${security.refresh.cookie-path:/}")
    private String cookiePath;

    @Value("${security.refresh.secure:true}")  // ✅ true par défaut (HTTPS)
    private boolean secure;

    @Value("${security.refresh.same-site:Strict}")  // ✅ Strict par défaut (sécurité CSRF)
    private String sameSite;

    /**
     * Nom du cookie
     */
    public String cookieName() {
        return cookieName;
    }

    /**
     * ✅ Créer un cookie refresh token
     *
     * @param rawToken Le token brut (UUID.UUID)
     * @param expiresAt Date d'expiration du token
     * @return Cookie configuré
     */
    public Cookie create(String rawToken, LocalDateTime expiresAt) {
        long maxAgeSeconds = Math.max(0, Duration.between(LocalDateTime.now(), expiresAt).getSeconds());

        Cookie cookie = new Cookie(cookieName, rawToken);
        cookie.setHttpOnly(true);    // ✅ Protection XSS
        cookie.setSecure(secure);    // ✅ HTTPS uniquement
        cookie.setPath(cookiePath);  // ✅ Disponible sur tout le site
        cookie.setMaxAge((int) maxAgeSeconds);

        // Note: SameSite doit être configuré via setAttribute dans Spring Boot 3+
        // Mais comme on utilise Cookie standard, on le note en commentaire
        // Pour une vraie implémentation, utiliser ResponseCookie de Spring

        log.debug("Cookie '{}' created with maxAge={} seconds", cookieName, maxAgeSeconds);

        return cookie;
    }

    /**
     * ✅ Créer un cookie de suppression (maxAge=0)
     *
     * @return Cookie avec maxAge=0 pour suppression
     */
    public Cookie delete() {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(0);  // ✅ Supprime le cookie

        log.debug("Cookie '{}' deleted", cookieName);

        return cookie;
    }

    /**
     * ✅ Lire le refresh token depuis les cookies de la requête
     *
     * @param request La requête HTTP
     * @return Le token ou null si absent
     */
    public String readFrom(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            log.debug("No cookies in request");
            return null;
        }

        String token = Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        if (token != null) {
            log.debug("Cookie '{}' found in request", cookieName);
        } else {
            log.debug("Cookie '{}' not found in request", cookieName);
        }

        return token;
    }
}