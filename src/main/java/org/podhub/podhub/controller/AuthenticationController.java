package org.podhub.podhub.controller;

import lombok.RequiredArgsConstructor;
import org.podhub.podhub.dto.*;
import org.podhub.podhub.exception.InvalidRefreshTokenException;
import org.podhub.podhub.security.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) throws InvalidRefreshTokenException {
        return ResponseEntity.ok(authService.refresh(request));
    }
}
