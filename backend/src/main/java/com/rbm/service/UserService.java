// UserService.java
package com.rbm.service;

import com.rbm.dto.UserRequest;
import com.rbm.dto.UserResponse;
import com.rbm.entity.ERole;
import com.rbm.entity.Role;
import com.rbm.entity.User;
import com.rbm.exception.BusinessException;
import com.rbm.exception.ResourceNotFoundException;
import com.rbm.repository.RoleRepository;
import com.rbm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UserResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public UserResponse create(UserRequest req) {
        if (req.password() == null || req.password().isBlank()) {
            throw new BusinessException("Le mot de passe est obligatoire");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessException("Cet email est déjà utilisé : " + req.email());
        }
        User user = new User();
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        applyCommon(user, req);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest req) {
        User user = getEntity(id);
        if (!user.getEmail().equals(req.email()) && userRepository.existsByEmail(req.email())) {
            throw new BusinessException("Cet email est déjà utilisé : " + req.email());
        }
        user.setEmail(req.email());
        if (req.password() != null && !req.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.password()));
        }
        applyCommon(user, req);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        userRepository.delete(getEntity(id));
    }

    // ---------- helpers ----------

    private void applyCommon(User user, UserRequest req) {
        user.setTel(req.tel());
        user.setNom(req.nom());
        user.setPrenom(req.prenom());

        // Par défaut : CLIENT
        Set<ERole> wanted = (req.roles() == null || req.roles().isEmpty())
                ? Set.of(ERole.CLIENT) : req.roles();
        Set<Role> roles = new HashSet<>();
        for (ERole r : wanted) {
            roles.add(roleRepository.findByLibelle(r)
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : " + r)));
        }
        user.setRoles(roles);
    }

    private User getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
    }

    private UserResponse toResponse(User u) {
        Set<ERole> roles = u.getRoles().stream().map(Role::getLibelle).collect(Collectors.toSet());
        return new UserResponse(u.getId(), u.getEmail(), u.getTel(), u.getNom(), u.getPrenom(), roles);
    }
}