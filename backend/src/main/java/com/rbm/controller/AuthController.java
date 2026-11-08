package com.rbm.controller;

import com.rbm.dto.AuthResponse;
import com.rbm.dto.LoginRequest;
import com.rbm.dto.PasswordChangeRequest;
import com.rbm.dto.ProfileUpdateRequest;
import com.rbm.dto.RegisterRequest;
import com.rbm.dto.UserResponse;
import com.rbm.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(req));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return service.login(req);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return service.me(authentication.getName());
    }
        @PutMapping("/me")
    public UserResponse updateMe(Authentication authentication, @Valid @RequestBody ProfileUpdateRequest req) {
        return service.updateProfile(authentication.getName(), req);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(Authentication authentication,
                                               @Valid @RequestBody PasswordChangeRequest req) {
        service.changePassword(authentication.getName(), req);
        return ResponseEntity.noContent().build();
    }
}