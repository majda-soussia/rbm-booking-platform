package com.rbm.service;

import com.rbm.entity.EmailVerificationToken;
import com.rbm.entity.User;
import com.rbm.event.VerificationEmailEvent;
import com.rbm.exception.BusinessException;
import com.rbm.repository.EmailVerificationTokenRepository;
import com.rbm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final Duration VALIDITE = Duration.ofHours(24);
    private static final Duration DELAI_RENVOI = Duration.ofSeconds(60);

    private final EmailVerificationTokenRepository tokens;
    private final UserRepository users;
    private final ApplicationEventPublisher events;
    private final SecureRandom random = new SecureRandom();

    /** Crée un nouveau jeton (l'ancien est supprimé) ; l'email part après validation de la transaction. */
    @Transactional
    public void envoyerLien(User user) {
        tokens.deleteByUser(user);
        tokens.flush();
        byte[] octets = new byte[32];
        random.nextBytes(octets);
        String brut = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);

        EmailVerificationToken t = new EmailVerificationToken();
        t.setUser(user);
        t.setTokenHash(hash(brut));
        t.setCreatedAt(LocalDateTime.now());
        t.setExpiresAt(LocalDateTime.now().plus(VALIDITE));
        tokens.save(t);

        events.publishEvent(new VerificationEmailEvent(user.getEmail(), user.getPrenom(), brut));
    }

    @Transactional
    public void verifier(String brut) {
        EmailVerificationToken t = tokens.findByTokenHash(hash(brut))
                .orElseThrow(() -> new BusinessException("Lien de vérification invalide ou déjà utilisé."));
        if (t.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Ce lien a expiré. Demandez un nouvel email de vérification.");
        }
        User user = t.getUser();
        user.setEmailVerifie(true);
        users.save(user);
        tokens.delete(t);
    }

    /** Même réponse que le compte existe ou non : on ne révèle pas quelles adresses sont inscrites. */
    @Transactional
    public void renvoyer(String email) {
        users.findByEmail(email.trim().toLowerCase())
                .filter(u -> !u.estVerifie())
                .ifPresent(u -> {
                    boolean recent = tokens.findFirstByUserOrderByCreatedAtDesc(u)
                            .map(t -> t.getCreatedAt().isAfter(LocalDateTime.now().minus(DELAI_RENVOI)))
                            .orElse(false);
                    if (!recent) {
                        envoyerLien(u);
                    }
                });
    }

    private static String hash(String brut) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(brut.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}