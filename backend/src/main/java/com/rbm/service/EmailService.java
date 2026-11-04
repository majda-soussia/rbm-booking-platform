package com.rbm.service;

import com.rbm.entity.Equipement;
import com.rbm.entity.Reservation;
import com.rbm.entity.Salle;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter HEURE_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final JavaMailSender mailSender;
    private final FileStorageService fileStorageService;

    @Value("${app.mail.from:no-reply@rbm.com}")
    private String from;

    /** Lève une exception si l'envoi échoue : l'appelant enregistre alors le statut ECHEC. */
    public void envoyerInvitation(Reservation r, String emailInvite, String nomInvite) throws Exception {
        Salle salle = r.getSalle();
        Optional<Path> photo = fileStorageService.resolve(salle.getPhotoUrl());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(emailInvite);
        helper.setSubject("Invitation : " + r.getTitre());
        helper.setText(construireHtml(r, salle, nomInvite, photo.isPresent()), true);
        if (photo.isPresent()) {
            helper.addInline("photoSalle", new FileSystemResource(photo.get()));
        }
        mailSender.send(message);
    }

    private String construireHtml(Reservation r, Salle salle, String nomInvite, boolean avecPhoto) {
        String equipements = salle.getEquipements().isEmpty() ? "Aucun"
                : salle.getEquipements().stream().map(Equipement::getLibelle)
                    .map(HtmlUtils::htmlEscape).collect(Collectors.joining(", "));

        String date = r.getDateDebut().toLocalDate().equals(r.getDateFin().toLocalDate())
                ? r.getDateDebut().format(DATE_FMT)
                : r.getDateDebut().format(DATE_FMT) + " au " + r.getDateFin().format(DATE_FMT);

        String organisateur = HtmlUtils.htmlEscape(r.getUser().getPrenom() + " " + r.getUser().getNom());
        String salut = (nomInvite == null || nomInvite.isBlank())
                ? "Bonjour," : "Bonjour " + HtmlUtils.htmlEscape(nomInvite) + ",";

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:Arial,sans-serif;max-width:600px;margin:auto;color:#222'>");
        sb.append("<h2 style='color:#1e5eff'>Vous êtes invité(e)</h2>");
        sb.append("<p>").append(salut).append("</p>");
        sb.append("<p><b>").append(organisateur).append("</b> vous invite à l'événement suivant :</p>");

        sb.append("<table style='border-collapse:collapse;width:100%'>");
        ligne(sb, "Titre", HtmlUtils.htmlEscape(r.getTitre()));
        if (r.getDescription() != null && !r.getDescription().isBlank()) {
            ligne(sb, "Description", HtmlUtils.htmlEscape(r.getDescription()));
        }
        ligne(sb, "Type", r.getType().name());
        ligne(sb, "Date", date);
        ligne(sb, "Horaires", r.getDateDebut().format(HEURE_FMT) + " – " + r.getDateFin().format(HEURE_FMT));
        ligne(sb, "Organisateur", organisateur);
        ligne(sb, "Salle", HtmlUtils.htmlEscape(salle.getNom()));
        ligne(sb, "Étage", salle.getEtage() == null ? "—" : String.valueOf(salle.getEtage()));
        ligne(sb, "Capacité", salle.getCapacite() + " personnes");
        ligne(sb, "Équipements", equipements);
        if (salle.getAdresse() != null && !salle.getAdresse().isBlank()) {
            ligne(sb, "Adresse", HtmlUtils.htmlEscape(salle.getAdresse()));
        }
        sb.append("</table>");

        if (salle.getLocalisationUrl() != null && !salle.getLocalisationUrl().isBlank()) {
            sb.append("<p><a href='").append(HtmlUtils.htmlEscape(salle.getLocalisationUrl()))
              .append("'>Voir la salle sur la carte</a></p>");
        }
        if (avecPhoto) {
            sb.append("<p><img src='cid:photoSalle' alt='Photo de la salle' style='max-width:100%;border-radius:8px'/></p>");
        }
        sb.append("<p style='color:#888;font-size:12px'>Vous n'avez pas besoin de compte : ce message est une simple invitation.</p>");
        sb.append("</div>");
        return sb.toString();
    }

    private void ligne(StringBuilder sb, String label, String valeur) {
        sb.append("<tr><td style='padding:6px 10px;border-bottom:1px solid #eee;font-weight:bold;width:130px'>")
          .append(label)
          .append("</td><td style='padding:6px 10px;border-bottom:1px solid #eee'>")
          .append(valeur)
          .append("</td></tr>");
    }
}