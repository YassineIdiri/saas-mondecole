package com.example.mondecole_pocket.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ✅ Wrapper le body HTTP pour pouvoir le lire plusieurs fois
 */
@Slf4j
public class CachedBodyFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        // ✅ Uniquement pour POST sur /api/auth/login et /api/auth/register
        if (!"POST".equals(method)) {
            return true;
        }

        return !path.equals("/api/auth/login") &&
                !path.equals("/api/auth/register");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // ✅ Wrapper la request
            CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);

            log.info("✅ CachedBodyFilter: Body cached pour {}", request.getServletPath());
            log.info("📄 CachedBodyFilter: Body content = {}", cachedRequest.getBody());

            // ✅ Passer la request wrappée à TOUS les filtres suivants
            filterChain.doFilter(cachedRequest, response);

        } catch (IOException e) {
            log.error("❌ CachedBodyFilter: Erreur lors du wrapping: {}", e.getMessage());
            throw e;
        }
    }
}