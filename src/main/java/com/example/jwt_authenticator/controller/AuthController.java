package com.example.jwt_authenticator.controller;

import com.example.jwt_authenticator.security.RefreshCookieFactory;
import com.example.jwt_authenticator.dto.AuthResponse;
import com.example.jwt_authenticator.dto.LoginRequest;
import com.example.jwt_authenticator.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieFactory refreshCookieFactory;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req, HttpServletRequest httpReq) {
        var result = authService.login(req, httpReq);

        return ResponseEntity.ok()
                .header("Set-Cookie", refreshCookieFactory.create(result.refreshTokenRaw(), result.refreshExpiresAt()).toString())
                .body(AuthService.toResponse(result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest httpReq) {
        String refreshToken = refreshCookieFactory.readFrom(httpReq);
        var result = authService.refresh(refreshToken, httpReq);

        return ResponseEntity.ok()
                .header("Set-Cookie", refreshCookieFactory.create(result.refreshTokenRaw(), result.refreshExpiresAt()).toString())
                .body(AuthService.toResponse(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpReq) {
        String refreshToken = refreshCookieFactory.readFrom(httpReq);
        authService.logout(refreshToken);

        return ResponseEntity.noContent()
                .header("Set-Cookie", refreshCookieFactory.delete().toString())
                .build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(HttpServletRequest httpReq) {
        String refreshToken = refreshCookieFactory.readFrom(httpReq);
        authService.logoutAll(refreshToken);

        return ResponseEntity.noContent()
                .header("Set-Cookie", refreshCookieFactory.delete().toString())
                .build();
    }
}
