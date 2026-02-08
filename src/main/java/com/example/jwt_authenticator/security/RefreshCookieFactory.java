package com.example.jwt_authenticator.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {

    @Value("${security.refresh.cookie-name:refresh_token}")
    private String cookieName;

    @Value("${security.refresh.cookie-path:/api/auth}")
    private String cookiePath;

    @Value("${security.refresh.secure:false}")
    private boolean secure;

    @Value("${security.refresh.same-site:Lax}")
    private String sameSite;

    public String cookieName() {
        return cookieName;
    }

    public ResponseCookie create(String rawToken, LocalDateTime expiresAt) {
        long maxAgeSeconds = Math.max(0, Duration.between(LocalDateTime.now(), expiresAt).getSeconds());

        return ResponseCookie.from(cookieName, rawToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(cookiePath)
                .maxAge(maxAgeSeconds)
                .build();
    }

    public ResponseCookie delete() {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(cookiePath)
                .maxAge(0)
                .build();
    }

    public String readFrom(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) return null;

        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
