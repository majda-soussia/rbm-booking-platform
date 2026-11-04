package com.rbm.service;

import com.rbm.entity.ERole;
import com.rbm.entity.User;
import com.rbm.event.UserRegisteredEvent;
import com.rbm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class AdminNotificationServiceTest {

    @Mock UserRepository userRepository;
    @Mock JavaMailSender mailSender;

    private AdminNotificationService service;

    @BeforeEach
    void setUp() {
        service = new AdminNotificationService(userRepository, mailSender);
        ReflectionTestUtils.setField(service, "from", "no-reply@rbm.com");
    }

    private User admin(String email) {
        User u = new User();
        u.setEmail(email);
        return u;
    }

    private UserRegisteredEvent evenement(String prenom, String nom) {
        return new UserRegisteredEvent("majda@test.com", nom, prenom, "20123456", LocalDateTime.now());
    }

    @Test
    @DisplayName("Chaque admin reçoit un email avec les informations du nouvel inscrit")
    void notifieTousLesAdmins() {
        when(userRepository.findByRoles_Libelle(ERole.ADMIN))
                .thenReturn(List.of(admin("a1@rbm.com"), admin("a2@rbm.com")));

        service.onUserRegistered(evenement("Majda", "Soussi"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(captor.capture());
        SimpleMailMessage premier = captor.getAllValues().get(0);
        assertThat(premier.getTo()).containsExactly("a1@rbm.com");
        assertThat(premier.getSubject()).isEqualTo("Nouvelle inscription : Majda Soussi");
        assertThat(premier.getText()).contains("majda@test.com").contains("20123456");
    }

    @Test
    @DisplayName("Aucun admin : aucun email, aucune erreur")
    void aucunAdmin() {
        when(userRepository.findByRoles_Libelle(ERole.ADMIN)).thenReturn(List.of());

        assertThatCode(() -> service.onUserRegistered(evenement("Majda", "Soussi"))).doesNotThrowAnyException();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Panne du serveur mail : l'erreur n'est pas propagée")
    void panneMail() {
        when(userRepository.findByRoles_Libelle(ERole.ADMIN)).thenReturn(List.of(admin("a1@rbm.com")));
        doThrow(new MailSendException("SMTP indisponible")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> service.onUserRegistered(evenement("Majda", "Soussi"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Retours à la ligne dans le nom : pas d'injection d'en-têtes dans le sujet")
    void injectionEntete() {
        when(userRepository.findByRoles_Libelle(ERole.ADMIN)).thenReturn(List.of(admin("a1@rbm.com")));

        service.onUserRegistered(evenement("Majda\r\nBcc: pirate@evil.com", "Soussi"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getSubject()).doesNotContain("\r").doesNotContain("\n");
    }
}