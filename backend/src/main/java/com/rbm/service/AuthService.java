package com.rbm.service;

import com.rbm.dto.AuthResponse;
import com.rbm.dto.LoginRequest;
import com.rbm.dto.PasswordChangeRequest;
import com.rbm.dto.ProfileUpdateRequest;
import com.rbm.dto.RegisterRequest;
import com.rbm.dto.UserResponse;
import com.rbm.entity.ERole;
import com.rbm.entity.Role;
import com.rbm.entity.User;
import com.rbm.event.UserRegisteredEvent;
import com.rbm.exception.AuthenticationFailedException;
import com.rbm.exception.BusinessException;
import com.rbm.exception.ResourceNotFoundException;
import com.rbm.repository.RoleRepository;
import com.rbm.repository.UserRepository;
import com.rbm.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;   // <-- AJOUTE CETTE LIGNE
        @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Cet email est déjà utilisé : " + email);
        }
        Role clientRole = roleRepository.findByLibelle(ERole.CLIENT)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : CLIENT"));

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setTel(req.tel());
        user.setNom(req.nom());
        user.setPrenom(req.prenom());
        user.setRoles(new HashSet<>(Set.of(clientRole)));   // jamais de rôle venant du client

        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new com.rbm.event.UserRegisteredEvent(
                saved.getEmail(), saved.getNom(), saved.getPrenom(), saved.getTel(),
                java.time.LocalDateTime.now()));
        return buildResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(AuthenticationFailedException::new);
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new AuthenticationFailedException();
        }
        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));
        return toUserResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        List<String> roles = user.getRoles().stream().map(r -> r.getLibelle().name()).toList();
        String token = jwtService.generateToken(user.getEmail(), roles);
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(), toUserResponse(user));
    }

    private UserResponse toUserResponse(User u) {
        Set<ERole> roles = u.getRoles().stream().map(Role::getLibelle).collect(Collectors.toSet());
        return new UserResponse(u.getId(), u.getEmail(), u.getTel(), u.getNom(), u.getPrenom(), roles);
    }
        @Transactional
    public UserResponse updateProfile(String email, ProfileUpdateRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));
        user.setNom(req.nom().trim());
        user.setPrenom(req.prenom().trim());
        user.setTel(req.tel() == null || req.tel().isBlank() ? null : req.tel().trim());
        return toUserResponse(userRepository.save(user));   // l'email et les rôles ne changent pas ici
    }

    @Transactional
    public void changePassword(String email, PasswordChangeRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));
        // 409 et non 401 : côté Angular, un 401 signifie « session expirée » et déconnecterait l'utilisateur
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new BusinessException("Le mot de passe actuel est incorrect");
        }
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new BusinessException("Le nouveau mot de passe doit être différent de l'ancien");
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }
}