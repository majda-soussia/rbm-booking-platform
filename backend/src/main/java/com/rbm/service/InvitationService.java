package com.rbm.service;

import com.rbm.dto.InvitationResponse;
import com.rbm.dto.InvitationsRequest;
import com.rbm.dto.InviteRequest;
import com.rbm.entity.Invitation;
import com.rbm.entity.Reservation;
import com.rbm.entity.StatutEnvoi;
import com.rbm.entity.StatutReservation;
import com.rbm.exception.BusinessException;
import com.rbm.exception.ForbiddenException;
import com.rbm.exception.ResourceNotFoundException;
import com.rbm.repository.InvitationRepository;
import com.rbm.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final ReservationRepository reservationRepository;
    private final EmailService emailService;

    public List<InvitationResponse> findByReservation(Long reservationId, String email, boolean admin) {
        Reservation r = getReservation(reservationId);
        verifierProprietaire(r, email, admin);
        return invitationRepository.findByReservationId(reservationId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<InvitationResponse> inviter(Long reservationId, InvitationsRequest req,
                                            String email, boolean admin) {
        Reservation r = getReservation(reservationId);
        verifierProprietaire(r, email, admin);

        // Invitation possible uniquement pour une réservation confirmée
        if (r.getStatut() != StatutReservation.CONFIRMEE) {
            throw new BusinessException("Les invitations ne sont possibles que pour une réservation confirmée");
        }

        // Doublons dans la requête et invités déjà notifiés
        Set<String> vus = new HashSet<>();
        for (InviteRequest inv : req.invites()) {
            String adresse = inv.email().trim().toLowerCase();
            if (!vus.add(adresse)) {
                throw new BusinessException("Adresse en double dans la liste : " + adresse);
            }
            if (invitationRepository.existsByReservationIdAndEmailInviteIgnoreCaseAndStatutEnvoi(
                    reservationId, adresse, StatutEnvoi.ENVOYEE)) {
                throw new BusinessException("Cette personne est déjà invitée : " + adresse);
            }
        }

        // Le nombre d'invités ne dépasse pas nbParticipants
        long deja = invitationRepository.countByReservationIdAndStatutEnvoi(reservationId, StatutEnvoi.ENVOYEE);
        if (deja + req.invites().size() > r.getNbParticipants()) {
            throw new BusinessException("Le nombre d'invités (" + (deja + req.invites().size())
                    + ") dépasse le nombre de participants prévu (" + r.getNbParticipants() + ")");
        }

        // Chaque envoi est enregistré avec son statut
        List<InvitationResponse> resultats = new ArrayList<>();
        for (InviteRequest inv : req.invites()) {
            Invitation invitation = new Invitation();
            invitation.setReservation(r);
            invitation.setEmailInvite(inv.email().trim());
            invitation.setNomInvite(inv.nom());
            invitation.setDateEnvoi(LocalDateTime.now());
            try {
                emailService.envoyerInvitation(r, inv.email().trim(), inv.nom());
                invitation.setStatutEnvoi(StatutEnvoi.ENVOYEE);
            } catch (Exception e) {
                invitation.setStatutEnvoi(StatutEnvoi.ECHEC);
            }
            resultats.add(toResponse(invitationRepository.save(invitation)));
        }
        return resultats;
    }

    private void verifierProprietaire(Reservation r, String email, boolean admin) {
        if (!admin && !r.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenException("Seul le créateur de la réservation (ou un admin) peut gérer ses invitations");
        }
    }

    private Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + id));
    }

    private InvitationResponse toResponse(Invitation i) {
        return new InvitationResponse(i.getId(), i.getEmailInvite(), i.getNomInvite(),
                i.getStatutEnvoi(), i.getDateEnvoi(), i.getReservation().getId());
    }
}