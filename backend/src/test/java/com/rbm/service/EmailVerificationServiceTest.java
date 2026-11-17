package com.rbm.service;

import com.rbm.entity.EmailVerificationToken;
import com.rbm.entity.User;
import com.rbm.event.VerificationEmailEvent;
import com.rbm.exception.BusinessException;
import com.rbm.repository.EmailVerificationTokenRepository;
import com.rbm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock EmailVerificationTokenRepository tokens;
    @Mock UserRepository users;
    @Mock ApplicationEventPublisher events;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(tokens, users, events);
    }

    private User compteNonVerifie() {
        User u = new User();
        u.setId(1L);
        u.setEmail("amel@test.com");
        u.setPrenom("Amel");
        u.setEmailVerifie(false);
        return u;
    }

    private EmailVerificationToken jeton(User u, LocalDateTime expiration, LocalDateTime creation) {
        EmailVerificationToken t = new EmailVerificationToken();
        t.setUser(u);
        t.setTokenHash("hash");
        t.setExpiresAt(expiration);
        t.setCreatedAt(creation);
        return t;
    }

    @Test
    @DisplayName("Envoi du lien : jeton haché (jamais en clair), valable 24 h, événement publié avec le jeton brut")
    void envoyerLien() {
        service.envoyerLien(compteNonVerifie());

        ArgumentCaptor<EmailVerificationToken> sauve = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokens).save(sauve.capture());
        ArgumentCaptor<VerificationEmailEvent> evenement = ArgumentCaptor.forClass(VerificationEmailEvent.class);
        verify(events).publishEvent(evenement.capture());

        String brut = evenement.getValue().token();
        assertThat(brut).isNotBlank();
        assertThat(sauve.getValue().getTokenHash()).hasSize(64).isNotEqualTo(brut);
        assertThat(sauve.getValue().getExpiresAt()).isAfter(LocalDateTime.now().plusHours(23));
        assertThat(evenement.getValue().email()).isEqualTo("amel@test.com");
    }

    @Test
    @DisplayName("Vérification : le compte est activé et le jeton supprimé (usage unique)")
    void verifier_ok() {
        User u = compteNonVerifie();
        EmailVerificationToken t = jeton(u, LocalDateTime.now().plusHours(1), LocalDateTime.now());
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(t));

        service.verifier("jeton-brut");

        assertThat(u.estVerifie()).isTrue();
        verify(users).save(u);
        verify(tokens).delete(t);
    }

    @Test
    @DisplayName("Vérification : lien inconnu ou déjà utilisé")
    void verifier_inconnu() {
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifier("inconnu"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalide");
        verifyNoInteractions(users);
    }

    @Test
    @DisplayName("Vérification : lien expiré, le compte reste non vérifié")
    void verifier_expire() {
        User u = compteNonVerifie();
        EmailVerificationToken t = jeton(u, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().minusDays(2));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.verifier("expire"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expiré");
        assertThat(u.estVerifie()).isFalse();
    }

    @Test
    @DisplayName("Renvoi : adresse inconnue, aucune action et aucune information révélée")
    void renvoyer_inconnu() {
        when(users.findByEmail("x@test.com")).thenReturn(Optional.empty());

        service.renvoyer("X@test.com");

        verifyNoInteractions(tokens, events);
    }

    @Test
    @DisplayName("Renvoi : refusé si le dernier email date de moins d'une minute")
    void renvoyer_tropRapide() {
        User u = compteNonVerifie();
        when(users.findByEmail("amel@test.com")).thenReturn(Optional.of(u));
        when(tokens.findFirstByUserOrderByCreatedAtDesc(u))
                .thenReturn(Optional.of(jeton(u, LocalDateTime.now().plusHours(23), LocalDateTime.now())));

        service.renvoyer("amel@test.com");

        verifyNoInteractions(events);
    }

    @Test
    @DisplayName("Renvoi : un nouvel email part si le précédent date de plus d'une minute")
    void renvoyer_ok() {
        User u = compteNonVerifie();
        when(users.findByEmail("amel@test.com")).thenReturn(Optional.of(u));
        when(tokens.findFirstByUserOrderByCreatedAtDesc(u))
                .thenReturn(Optional.of(jeton(u, LocalDateTime.now().plusHours(20), LocalDateTime.now().minusMinutes(5))));

        service.renvoyer("amel@test.com");

        verify(events).publishEvent(any(VerificationEmailEvent.class));
    }
}