package com.example.mondecole_pocket.service;

import com.example.mondecole_pocket.dto.*;
import com.example.mondecole_pocket.entity.User;
import com.example.mondecole_pocket.repository.UserRepository;
import com.example.mondecole_pocket.security.CustomUserDetails;
import com.example.mondecole_pocket.security.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public record AuthResult(
            String accessToken,
            long expiresIn,
            String username,
            String refreshTokenRaw,
            LocalDateTime refreshExpiresAt
    ) {}

    // ════════════════════════════════════════════════════════
    // LOGIN - Connexion avec username + password
    // ════════════════════════════════════════════════════════

    /**
     * Login d'un utilisateur dans son organisation
     *
     * L'organizationId DOIT être fourni via :
     * - Header X-Organization-Id (vérifié par TenantFilter)
     *
     * Le TenantFilter a déjà configuré le TenantContext avant d'arriver ici
     */
    @Transactional
    public AuthResult login(LoginRequest req, HttpServletRequest httpReq) {

        // ✅ Récupérer l'organizationId du TenantContext (configuré par TenantFilter)
        Long organizationId = TenantContext.getTenantId();

        if (organizationId == null) {
            log.error("❌ TenantContext vide lors du login - Probable bug dans TenantFilter");
            throw new IllegalStateException("Organization context not set");
        }

        log.debug("Login attempt: username={}, organizationId={}", req.username(), organizationId);

        // ✅ Chercher le user dans l'organisation spécifique
        User user = userRepository.findByUsernameAndOrganizationId(req.username(), organizationId)
                .orElseThrow(() -> {
                    log.warn("⚠️ Login failed: user '{}' not found in org {}",
                            req.username(), organizationId);
                    return new BadCredentialsException("INVALID_CREDENTIALS");
                });

        // Vérifier mot de passe
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            log.warn("⚠️ Login failed: invalid password for user '{}' in org {}",
                    req.username(), organizationId);
            throw new BadCredentialsException("INVALID_CREDENTIALS");
        }

        // Vérifier statut du compte
        assertAccountOk(user);

        // Mettre à jour last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // ✅ Générer access token avec userId + organizationId
        String accessToken = issueAccessToken(user);
        long expiresIn = jwtService.getTimeUntilExpirationSeconds(accessToken);

        // ✅ Créer refresh token avec userId + organizationId
        var issued = refreshTokenService.issue(
                user.getId(),
                user.getOrganization().getId(),
                req.rememberMe(),
                httpReq
        );

        log.info("✅ User '{}' logged in to organization {} (role: {})",
                user.getUsername(), organizationId, user.getRole());

        return new AuthResult(
                accessToken,
                expiresIn,
                user.getUsername(),
                issued.rawToken(),
                issued.expiresAt()
        );
    }

    // ════════════════════════════════════════════════════════
    // REFRESH - Renouvellement du token
    // ════════════════════════════════════════════════════════

    @Transactional
    public AuthResult refresh(String refreshTokenRaw, HttpServletRequest httpReq) {

        // ✅ Valider et faire la rotation du refresh token
        var rotated = refreshTokenService.rotate(refreshTokenRaw, httpReq);

        log.debug("Refreshing token: userId={}, organizationId={}",
                rotated.userId(), rotated.organizationId());

        // ✅ Charger le user
        User user = userRepository.findById(rotated.userId())
                .orElseThrow(() -> {
                    log.error("❌ User {} not found during refresh", rotated.userId());
                    return new BadCredentialsException("USER_NOT_FOUND");
                });

        // ✅ SÉCURITÉ CRITIQUE : Vérifier que l'org du user = org du refresh token
        if (!user.getOrganization().getId().equals(rotated.organizationId())) {
            log.error("🔴 SECURITY ALERT: Organization mismatch during refresh! " +
                            "User org: {}, Token org: {}, User: {}",
                    user.getOrganization().getId(), rotated.organizationId(), user.getUsername());
            throw new BadCredentialsException("ORGANIZATION_MISMATCH");
        }

        // Vérifier statut du compte
        assertAccountOk(user);

        // ✅ Générer nouveau access token
        String accessToken = issueAccessToken(user);
        long expiresIn = jwtService.getTimeUntilExpirationSeconds(accessToken);

        log.debug("✅ Token refreshed for user '{}' in org {}",
                user.getUsername(), user.getOrganization().getId());

        return new AuthResult(
                accessToken,
                expiresIn,
                user.getUsername(),
                rotated.rawToken(),
                rotated.expiresAt()
        );
    }

    // ════════════════════════════════════════════════════════
    // LOGOUT - Déconnexion
    // ════════════════════════════════════════════════════════

    public void logout(String refreshTokenRaw) {
        if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
            log.debug("Logout called with null/empty token");
            return;
        }
        refreshTokenService.revoke(refreshTokenRaw);
        log.debug("✅ User logged out (single device)");
    }

    public void logoutAll(String refreshTokenRaw) {
        if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
            throw new BadCredentialsException("INVALID_TOKEN");
        }
        Long userId = refreshTokenService.validate(refreshTokenRaw).getUserId();
        refreshTokenService.revokeAll(userId);
        log.info("✅ User {} logged out from all devices", userId);
    }

    // ════════════════════════════════════════════════════════
    // UTILITAIRES
    // ════════════════════════════════════════════════════════

    public static AuthResponse toResponse(AuthResult r) {
        return AuthResponse.of(r.accessToken(), r.expiresIn(), r.username());
    }

    private void assertAccountOk(User user) {
        if (user.isLocked()) {
            log.warn("⚠️ Account locked: {} (org: {})",
                    user.getUsername(), user.getOrganization().getId());
            throw new BadCredentialsException("ACCOUNT_LOCKED");
        }
        if (!user.isActive()) {
            log.warn("⚠️ Account disabled: {} (org: {})",
                    user.getUsername(), user.getOrganization().getId());
            throw new BadCredentialsException("ACCOUNT_DISABLED");
        }
    }

    private String issueAccessToken(User user) {
        var principal = new CustomUserDetails(
                user.getId(),
                user.getOrganization().getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.isActive(),
                user.isLocked(),
                user.getRole()
        );

        // ✅ Générer token avec userId + organizationId
        return jwtService.generateToken(
                principal.getId(),
                principal.getOrganizationId(),
                principal.getUsername(),
                principal.getAuthorities()
        );
    }
}