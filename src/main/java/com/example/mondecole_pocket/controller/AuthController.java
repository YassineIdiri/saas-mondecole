package com.example.mondecole_pocket.controller;

import com.example.mondecole_pocket.dto.*;
import com.example.mondecole_pocket.entity.User;
import com.example.mondecole_pocket.exception.UserNotFoundException;
import com.example.mondecole_pocket.repository.UserRepository;
import com.example.mondecole_pocket.security.RefreshCookieFactory;
import com.example.mondecole_pocket.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ════════════════════════════════════════════════════════
 * AUTH CONTROLLER
 * ════════════════════════════════════════════════════════
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final RefreshCookieFactory cookieFactory;

    // ════════════════════════════════════════════════════════
    // USER ORGANIZATION INFO
    // ════════════════════════════════════════════════════════

    /**
     * Récupérer les infos d'organisation d'un utilisateur
     *
     * Utilisé par le frontend pour savoir dans quelle org se connecter
     *
     * Exemple :
     * GET /api/auth/user-organization?username=john
     *
     * Response :
     * {
     *   "organizationId": 1,
     *   "organizationName": "Lycée Voltaire",
     *   "organizationSlug": "lycee-voltaire",
     *   "userRole": "STUDENT"
     * }
     */
    @GetMapping("/user-organization")
    public ResponseEntity<UserOrganizationInfo> getUserOrganization(@RequestParam String username) {
        log.debug("Getting organization info for username: {}", username);

        // ✅ Utiliser JOIN FETCH pour éviter LazyInitializationException
        User user = userRepository.findByUsernameWithOrganization(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UserNotFoundException("User not found");
                });

        // ✅ Organization est chargée, pas d'erreur
        UserOrganizationInfo info = new UserOrganizationInfo(
                user.getOrganization().getId(),
                user.getOrganization().getName(),
                user.getOrganization().getSlug(),
                user.getRole().name()
        );

        log.debug("Organization info retrieved: orgId={}, orgName={}",
                info.organizationId(), info.organizationName());

        return ResponseEntity.ok(info);
    }

    // ════════════════════════════════════════════════════════
    // LOGIN
    // ════════════════════════════════════════════════════════

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.info("Login request: username={}", request.username());

        AuthService.AuthResult result = authService.login(request, httpRequest);

        // ✅ Créer le cookie refresh token avec la factory
        Cookie refreshCookie = cookieFactory.create(
                result.refreshTokenRaw(),
                result.refreshExpiresAt()
        );
        httpResponse.addCookie(refreshCookie);

        AuthResponse response = AuthService.toResponse(result);

        log.info("✅ Login successful: username={}", result.username());

        return ResponseEntity.ok(response);
    }

    // ════════════════════════════════════════════════════════
    // REFRESH
    // ════════════════════════════════════════════════════════

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.debug("Refresh token request");

        String refreshToken = cookieFactory.readFrom(httpRequest);

        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("⚠️ Refresh failed: no refresh token in cookie");
            return ResponseEntity.status(401).build();
        }

        AuthService.AuthResult result = authService.refresh(refreshToken, httpRequest);

        Cookie newRefreshCookie = cookieFactory.create(
                result.refreshTokenRaw(),
                result.refreshExpiresAt()
        );
        httpResponse.addCookie(newRefreshCookie);

        AuthResponse response = AuthService.toResponse(result);

        log.debug("✅ Token refreshed successfully");

        return ResponseEntity.ok(response);
    }

    // ════════════════════════════════════════════════════════
    // LOGOUT
    // ════════════════════════════════════════════════════════

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.debug("Logout request");

        String refreshToken = cookieFactory.readFrom(httpRequest);

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        Cookie deleteCookie = cookieFactory.delete();
        httpResponse.addCookie(deleteCookie);

        log.debug("✅ User logged out");

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.debug("Logout all devices request");

        String refreshToken = cookieFactory.readFrom(httpRequest);

        if (refreshToken != null) {
            authService.logoutAll(refreshToken);
        }

        Cookie deleteCookie = cookieFactory.delete();
        httpResponse.addCookie(deleteCookie);

        log.info("✅ User logged out from all devices");

        return ResponseEntity.noContent().build();
    }
}