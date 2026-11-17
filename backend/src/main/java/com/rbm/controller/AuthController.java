package com.rbm.controller;

import com.rbm.dto.AuthResponse;
import com.rbm.dto.EmailRequest;
import com.rbm.dto.LoginRequest;
import com.rbm.dto.PasswordChangeRequest;
import com.rbm.dto.ProfileUpdateRequest;
import com.rbm.dto.RegisterRequest;
import com.rbm.dto.RegisterResponse;
import com.rbm.dto.TokenRequest;
import com.rbm.dto.UserResponse;
import com.rbm.service.AuthService;
import com.rbm.service.EmailVerificationService;

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
    private final EmailVerificationService verificationService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest req) {
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
    @PostMapping("/verify-email")
    public java.util.Map<String, String> verifyEmail(@Valid @RequestBody TokenRequest req) {
        verificationService.verifier(req.token());
        return java.util.Map.of("message", "Adresse email confirmée. Vous pouvez maintenant vous connecter.");
    }

    @PostMapping("/resend-verification")
    public java.util.Map<String, String> resend(@Valid @RequestBody EmailRequest req) {
        verificationService.renvoyer(req.email());
        return java.util.Map.of("message",
                "Si un compte non vérifié existe pour cette adresse, un nouvel email vient d'être envoyé.");
    }
}