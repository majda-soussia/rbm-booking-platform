package com.rbm.service;

import com.rbm.entity.ERole;
import com.rbm.entity.User;
import com.rbm.event.UserRegisteredEvent;
import com.rbm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@rbm.com}")
    private String from;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        List<User> admins = userRepository.findByRoles_Libelle(ERole.ADMIN);
        if (admins.isEmpty()) {
            log.warn("Nouvelle inscription ({}) : aucun administrateur à notifier", event.email());
            return;
        }

        String sujet = "Nouvelle inscription : " + nettoyer(event.prenom()) + " " + nettoyer(event.nom());
        String corps = """
                Un nouveau client vient de s'inscrire sur Room & Event Booking.

                Nom       : %s %s
                Email     : %s
                Téléphone : %s
                Date      : %s
                """.formatted(
                nettoyer(event.prenom()), nettoyer(event.nom()),
                nettoyer(event.email()),
                event.tel() == null || event.tel().isBlank() ? "non renseigné" : nettoyer(event.tel()),
                event.dateInscription().format(FMT));

        for (User admin : admins) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(admin.getEmail());
                message.setSubject(sujet);
                message.setText(corps);
                mailSender.send(message);
            } catch (Exception e) {
                // L'inscription est déjà validée : un échec d'email ne doit rien annuler
                log.warn("Échec de l'email de notification à {} : {}", admin.getEmail(), e.getMessage());
            }
        }
    }

    /** Supprime les retours à la ligne pour empêcher l'injection d'en-têtes dans l'email. */
    private String nettoyer(String valeur) {
        return valeur == null ? "" : valeur.replaceAll("[\\r\\n]+", " ").trim();
    }
}