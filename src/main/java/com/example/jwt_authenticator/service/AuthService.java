package com.example.jwt_authenticator.service;

import com.example.jwt_authenticator.dto.AuthResponse;
import com.example.jwt_authenticator.dto.LoginRequest;
import com.example.jwt_authenticator.entity.User;
import com.example.jwt_authenticator.repository.UserRepository;
import com.example.jwt_authenticator.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public record AuthResult(String accessToken, long expiresIn, String username,
                             String refreshTokenRaw, java.time.LocalDateTime refreshExpiresAt) {}

    public AuthResult login(LoginRequest req, HttpServletRequest httpReq) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new BadCredentialsException("INVALID_CREDENTIALS"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("INVALID_CREDENTIALS");
        }
        assertAccountOk(user);

        String accessToken = issueAccessToken(user);
        long expiresIn = jwtService.getTimeUntilExpirationSeconds(accessToken);

        var issued = refreshTokenService.issue(user.getId(), req.rememberMe(), httpReq);

        return new AuthResult(accessToken, expiresIn, user.getUsername(),
                issued.rawToken(), issued.expiresAt());
    }

    public AuthResult refresh(String refreshTokenRaw, HttpServletRequest httpReq) {
        var rotated = refreshTokenService.rotate(refreshTokenRaw, httpReq);

        User user = userRepository.findById(rotated.userId())
                .orElseThrow(() -> new BadCredentialsException("USER_NOT_FOUND"));

        assertAccountOk(user);

        String accessToken = issueAccessToken(user);
        long expiresIn = jwtService.getTimeUntilExpirationSeconds(accessToken);

        return new AuthResult(accessToken, expiresIn, user.getUsername(),
                rotated.rawToken(), rotated.expiresAt());
    }

    public void logout(String refreshTokenRaw) {
        refreshTokenService.revoke(refreshTokenRaw);
    }

    public void logoutAll(String refreshTokenRaw) {
        Long userId = refreshTokenService.validate(refreshTokenRaw).getUserId();
        refreshTokenService.revokeAll(userId);
    }

    public static AuthResponse toResponse(AuthResult r) {
        return AuthResponse.of(r.accessToken(), r.expiresIn(), r.username());
    }

    private void assertAccountOk(User user) {
        if (user.isLocked()) throw new BadCredentialsException("ACCOUNT_LOCKED");
        if (!user.isActive()) throw new BadCredentialsException("ACCOUNT_DISABLED");
    }

    private String issueAccessToken(User user) {
        var principal = new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.isActive(),
                user.isLocked(),
                user.getRole()
        );
        return jwtService.generateToken(principal);
    }
}
