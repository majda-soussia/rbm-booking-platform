package com.rbm.service;

import com.rbm.dto.CalendrierEvent;
import com.rbm.dto.ReservationRequest;
import com.rbm.dto.ReservationResponse;
import com.rbm.entity.Reservation;
import com.rbm.entity.Salle;
import com.rbm.entity.StatutReservation;
import com.rbm.entity.User;
import com.rbm.exception.BusinessException;
import com.rbm.exception.ForbiddenException;
import com.rbm.exception.ResourceNotFoundException;
import com.rbm.repository.InvitationRepository;
import com.rbm.repository.ReservationRepository;
import com.rbm.repository.SalleRepository;
import com.rbm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SalleRepository salleRepository;
    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;

    // ---------- Lecture ----------

    /** Réservé à l'admin (règle dans SecurityConfig). */
    public List<ReservationResponse> findAll() {
        return reservationRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<ReservationResponse> findMine(String email) {
        return reservationRepository.findByUserEmailOrderByDateDebutDesc(email)
                .stream().map(this::toResponse).toList();
    }

    public ReservationResponse findById(Long id, String email, boolean admin) {
        Reservation r = getEntity(id);
        verifierProprietaire(r, email, admin);
        return toResponse(r);
    }

    /** Admin : tous les détails. Client : ses réservations + les créneaux confirmés des autres, masqués. */
    public List<CalendrierEvent> findCalendrier(LocalDateTime debut, LocalDateTime fin,
                                                String email, boolean admin) {
        if (!fin.isAfter(debut)) {
            throw new BusinessException("La date de fin doit être postérieure à la date de début");
        }
        return reservationRepository.findByPeriode(debut, fin).stream()
                .map(r -> toEvent(r, email, admin))
                .filter(Objects::nonNull)
                .toList();
    }

    // ---------- Création / modification ----------

    @Transactional
    public ReservationResponse create(ReservationRequest req, String email) {
        Salle salle = getSalle(req.salleId());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + email));

        validerCreneau(salle, req.dateDebut(), req.dateFin(), req.nbParticipants(), null);

        Reservation r = new Reservation();
        r.setUser(user);
        r.setStatut(StatutReservation.EN_ATTENTE);   // règle 5
        apply(r, req, salle);
        return toResponse(reservationRepository.save(r));
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationRequest req, String email, boolean admin) {
        Reservation r = getEntity(id);
        verifierProprietaire(r, email, admin);
        if (r.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new BusinessException("Seule une demande en attente peut être modifiée");
        }
        Salle salle = getSalle(req.salleId());
        validerCreneau(salle, req.dateDebut(), req.dateFin(), req.nbParticipants(), id);
        apply(r, req, salle);
        return toResponse(reservationRepository.save(r));
    }

    /** Réservé à l'admin (règle dans SecurityConfig). */
    @Transactional
    public void delete(Long id) {
        Reservation r = getEntity(id);
        if (r.getStatut() == StatutReservation.CONFIRMEE) {
            throw new BusinessException("Une réservation confirmée doit d'abord être annulée avant d'être supprimée");
        }
        invitationRepository.deleteByReservationId(id);
        reservationRepository.delete(r);
    }

    // ---------- Workflow des statuts ----------

    /** Réservé à l'admin (règle dans SecurityConfig). */
    @Transactional
    public ReservationResponse confirmer(Long id) {
        Reservation r = getEntity(id);
        if (r.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new BusinessException("Seule une demande en attente peut être confirmée");
        }
        // La confirmation revérifie l'absence de conflit
        long conflits = reservationRepository.countConflits(
                r.getSalle().getId(), StatutReservation.CONFIRMEE, r.getDateDebut(), r.getDateFin(), r.getId());
        if (conflits > 0) {
            throw new BusinessException("Salle déjà réservée sur ce créneau");
        }
        r.setStatut(StatutReservation.CONFIRMEE);
        return toResponse(reservationRepository.save(r));
    }

    /** Réservé à l'admin (règle dans SecurityConfig). */
    @Transactional
    public ReservationResponse refuser(Long id) {
        Reservation r = getEntity(id);
        if (r.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new BusinessException("Seule une demande en attente peut être refusée");
        }
        r.setStatut(StatutReservation.REFUSEE);
        return toResponse(reservationRepository.save(r));
    }

    @Transactional
    public ReservationResponse annuler(Long id, String email, boolean admin) {
        Reservation r = getEntity(id);
        verifierProprietaire(r, email, admin);
        if (r.getStatut() != StatutReservation.EN_ATTENTE && r.getStatut() != StatutReservation.CONFIRMEE) {
            throw new BusinessException("Cette réservation ne peut plus être annulée (statut : " + r.getStatut() + ")");
        }
        // Le client n'annule qu'avant le début ; l'admin peut toujours annuler
        if (!admin && !r.getDateDebut().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Une réservation déjà commencée ne peut plus être annulée");
        }
        r.setStatut(StatutReservation.ANNULEE);
        return toResponse(reservationRepository.save(r));
    }

    // ---------- Règles de gestion ----------

    private void validerCreneau(Salle salle, LocalDateTime debut, LocalDateTime fin,
                                int nbParticipants, Long excludeId) {
        // Règle 1 : dates valides
        if (!fin.isAfter(debut)) {
            throw new BusinessException("La date de fin doit être strictement postérieure à la date de début");
        }
        if (debut.isBefore(LocalDateTime.now())) {
            throw new BusinessException("La date de début ne peut pas être dans le passé");
        }
        // Pas de 30 minutes, durée minimale 1 heure
        if (!estPas30Min(debut) || !estPas30Min(fin)) {
            throw new BusinessException("Les créneaux se réservent par pas de 30 minutes (ex : 09:00, 09:30)");
        }
        if (Duration.between(debut, fin).toMinutes() < 60) {
            throw new BusinessException("La durée minimale d'une réservation est de 1 heure");
        }
        // Règle 4 : salle active
        if (!salle.isActive()) {
            throw new BusinessException("Cette salle est désactivée et ne peut pas être réservée");
        }
        // Règle 3 : capacité
        if (nbParticipants > salle.getCapacite()) {
            throw new BusinessException("Le nombre de participants (" + nbParticipants
                    + ") dépasse la capacité de la salle (" + salle.getCapacite() + ")");
        }
        // Règle 2 : pas de chevauchement avec une réservation confirmée
        long conflits = reservationRepository.countConflits(
                salle.getId(), StatutReservation.CONFIRMEE, debut, fin, excludeId == null ? 0L : excludeId);
        if (conflits > 0) {
            throw new BusinessException("Salle déjà réservée sur ce créneau");
        }
    }

    private boolean estPas30Min(LocalDateTime d) {
        return d.getMinute() % 30 == 0 && d.getSecond() == 0 && d.getNano() == 0;
    }

    // prixTotal = durée en heures x tarifHoraire (calculé côté backend)
    static BigDecimal calculerPrix(LocalDateTime debut, LocalDateTime fin, BigDecimal tarifHoraire) {
        long minutes = Duration.between(debut, fin).toMinutes();
        BigDecimal heures = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return tarifHoraire.multiply(heures).setScale(2, RoundingMode.HALF_UP);
    }

    // ---------- Droits ----------

    private void verifierProprietaire(Reservation r, String email, boolean admin) {
        if (!admin && !r.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ForbiddenException("Cette réservation ne vous appartient pas");
        }
    }

    // ---------- helpers ----------

    private void apply(Reservation r, ReservationRequest req, Salle salle) {
        r.setTitre(req.titre());
        r.setDescription(req.description());
        r.setType(req.type());
        r.setDateDebut(req.dateDebut());
        r.setDateFin(req.dateFin());
        r.setNbParticipants(req.nbParticipants());
        r.setSalle(salle);
        // Le prix est figé dans la réservation : un changement futur du tarif de la salle ne l'affecte pas
        r.setPrixTotal(calculerPrix(req.dateDebut(), req.dateFin(), salle.getTarifHoraire()));
    }

    private Reservation getEntity(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + id));
    }

    private Salle getSalle(Long id) {
        return salleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable : " + id));
    }

    private CalendrierEvent toEvent(Reservation r, String email, boolean admin) {
        boolean mine = r.getUser().getEmail().equalsIgnoreCase(email);
        if (admin || mine) {
            return new CalendrierEvent(r.getId(), r.getSalle().getId(), r.getSalle().getNom(),
                    r.getDateDebut(), r.getDateFin(), r.getStatut(), r.getTitre(),
                    r.getUser().getPrenom() + " " + r.getUser().getNom(), mine);
        }
        if (r.getStatut() == StatutReservation.CONFIRMEE) {
            // Créneau occupé : ni titre, ni nom, ni identifiant
            return new CalendrierEvent(null, r.getSalle().getId(), r.getSalle().getNom(),
                    r.getDateDebut(), r.getDateFin(), StatutReservation.CONFIRMEE, "Occupé", null, false);
        }
        return null;   // les demandes des autres (en attente, refusées, annulées) ne regardent pas ce client
    }

    private ReservationResponse toResponse(Reservation r) {
        return new ReservationResponse(
                r.getId(), r.getTitre(), r.getDescription(), r.getType(),
                r.getDateDebut(), r.getDateFin(), r.getNbParticipants(), r.getStatut(),
                r.getPrixTotal(), r.getDateCreation(),
                r.getSalle().getId(), r.getSalle().getNom(),
                r.getUser().getId(), r.getUser().getPrenom() + " " + r.getUser().getNom());
    }
}