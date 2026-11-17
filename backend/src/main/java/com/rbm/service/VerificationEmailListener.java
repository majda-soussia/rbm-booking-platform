package com.rbm.service;

import com.rbm.event.VerificationEmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationEmailListener {
    private final MailGateway mail;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationEmail(VerificationEmailEvent e) {
        String lien = frontendUrl + "/#/verify-email?token=" + e.token();
        String prenom = HtmlUtils.htmlEscape(e.prenom() == null ? "" : e.prenom());
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:560px;margin:auto;color:#222">
                  <h2 style="color:#1e5eff">Confirmez votre adresse email</h2>
                  <p>Bonjour %s,</p>
                  <p>Merci de votre inscription sur <b>Room &amp; Event Booking</b>.
                     Cliquez sur le bouton pour activer votre compte (lien valable 24 heures) :</p>
                  <p><a href="%s" style="background:#1e5eff;color:#fff;padding:12px 22px;border-radius:6px;text-decoration:none;display:inline-block">Activer mon compte</a></p>
                  <p style="font-size:13px;color:#666">Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br>%s</p>
                  <p style="font-size:12px;color:#999">Si vous n'êtes pas à l'origine de cette inscription, ignorez ce message.</p>
                </div>
                """.formatted(prenom, lien, lien);
        try {
            mail.send(e.email(), "Confirmez votre inscription – Room & Event Booking", html);
        } catch (Exception ex) {
            log.warn("Échec de l'email de vérification à {} : {}", e.email(), ex.getMessage());
        }
    }
}