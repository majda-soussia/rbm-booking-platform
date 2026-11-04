package com.rbm.service;

import com.rbm.dto.AuthResponse;
import com.rbm.dto.LoginRequest;
import com.rbm.dto.RegisterRequest;
import com.rbm.entity.ERole;
import com.rbm.entity.Role;
import com.rbm.entity.User;
import com.rbm.event.UserRegisteredEvent;
import com.rbm.exception.AuthenticationFailedException;
import com.rbm.exception.BusinessException;
import com.rbm.repository.RoleRepository;
import com.rbm.repository.UserRepository;
import com.rbm.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ApplicationEventPublisher eventPublisher;

    private final JwtService jwtService =
            new JwtService("test-secret-key-with-at-least-32-characters-long", 60_000);

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService, eventPublisher);
    }

    private User utilisateur(String email, String hash, ERole role) {
        User u = new User();
        u.setId(1L);
        u.setEmail(email);
        u.setPassword(hash);
        u.setNom("Test");
        u.setPrenom("User");
        u.setRoles(new HashSet<>(Set.of(new Role(1L, role))));
        return u;
    }

    @Test
    @DisplayName("Inscription : compte CLIENT, mot de passe haché, email normalisé, token valide, admin notifié")
    void inscription_ok() {
        when(userRepository.existsByEmail("sami@test.com")).thenReturn(false);
        when(roleRepository.findByLibelle(ERole.CLIENT)).thenReturn(Optional.of(new Role(1L, ERole.CLIENT)));
        when(passwordEncoder.encode("motdepasse1")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(5L);
            return u;
        });

        AuthResponse res = service.register(
                new RegisterRequest("  Sami@Test.com ", "motdepasse1", "20123456", "Ben Ali", "Sami"));

        assertThat(res.user().email()).isEqualTo("sami@test.com");
        assertThat(res.user().roles()).containsExactly(ERole.CLIENT);
        assertThat(res.type()).isEqualTo("Bearer");
        assertThat(jwtService.isValid(res.token())).isTrue();
        assertThat(jwtService.extractRoles(res.token())).containsExactly("CLIENT");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("HASH");   // jamais le mot de passe en clair

        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    @DisplayName("Inscription refusée si l'email existe déjà : ni compte, ni notification")
    void inscription_emailExistant() {
        when(userRepository.existsByEmail("sami@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterRequest("sami@test.com", "motdepasse1", null, "Ben Ali", "Sami")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà utilisé");

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Connexion : token contenant les rôles de l'utilisateur")
    void connexion_ok() {
        when(userRepository.findByEmail("admin@rbm.com"))
                .thenReturn(Optional.of(utilisateur("admin@rbm.com", "HASH", ERole.ADMIN)));
        when(passwordEncoder.matches("Admin123!", "HASH")).thenReturn(true);

        AuthResponse res = service.login(new LoginRequest("Admin@rbm.com", "Admin123!"));

        assertThat(jwtService.isValid(res.token())).isTrue();
        assertThat(jwtService.extractEmail(res.token())).isEqualTo("admin@rbm.com");
        assertThat(jwtService.extractRoles(res.token())).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("Connexion refusée avec un mauvais mot de passe")
    void connexion_mauvaisMotDePasse() {
        when(userRepository.findByEmail("admin@rbm.com"))
                .thenReturn(Optional.of(utilisateur("admin@rbm.com", "HASH", ERole.ADMIN)));
        when(passwordEncoder.matches("faux", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("admin@rbm.com", "faux")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    @DisplayName("Connexion refusée pour un email inconnu (même message, hachage non sollicité)")
    void connexion_emailInconnu() {
        when(userRepository.findByEmail("inconnu@rbm.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("inconnu@rbm.com", "x")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Email ou mot de passe incorrect");
        verifyNoInteractions(passwordEncoder);
    }
}