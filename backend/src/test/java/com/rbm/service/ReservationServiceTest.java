package com.rbm.service;

import com.rbm.dto.CalendrierEvent;
import com.rbm.dto.ReservationRequest;
import com.rbm.dto.ReservationResponse;
import com.rbm.entity.*;
import com.rbm.exception.BusinessException;
import com.rbm.exception.ForbiddenException;
import com.rbm.repository.InvitationRepository;
import com.rbm.repository.ReservationRepository;
import com.rbm.repository.SalleRepository;
import com.rbm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final String EMAIL = "sami@test.com";
    private static final String AUTRE_EMAIL = "autre@test.com";
    private static final String ADMIN_EMAIL = "admin@rbm.com";

    @Mock ReservationRepository reservationRepository;
    @Mock SalleRepository salleRepository;
    @Mock UserRepository userRepository;
    @Mock InvitationRepository invitationRepository;

    @InjectMocks ReservationService service;

    private Salle salle;
    private User user;     // le client connecté dans les tests
    private User autre;    // un autre client
    private LocalDateTime debut;   // dans le futur, à 09:00 pile

    @BeforeEach
    void setUp() {
        salle = new Salle();
        salle.setId(1L);
        salle.setNom("Salle Carthage");
        salle.setCapacite(50);
        salle.setTarifHoraire(new BigDecimal("80.00"));
        salle.setActive(true);

        user = new User();
        user.setId(2L);
        user.setEmail(EMAIL);
        user.setNom("Ben Ali");
        user.setPrenom("Sami");

        autre = new User();
        autre.setId(3L);
        autre.setEmail(AUTRE_EMAIL);
        autre.setNom("Trabelsi");
        autre.setPrenom("Sarra");

        debut = LocalDateTime.now().plusDays(10).withHour(9).withMinute(0).withSecond(0).withNano(0);
    }

    private ReservationRequest request(Long salleId, LocalDateTime d, LocalDateTime f, int nb) {
        return new ReservationRequest("Formation", "Description", TypeReservation.FORMATION, d, f, nb, salleId);
    }

    private void stubSalleEtUser() {
        when(salleRepository.findById(1L)).thenReturn(Optional.of(salle));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    private void stubConflits(Long salleId, long resultat) {
        when(reservationRepository.countConflits(
                eq(salleId), eq(StatutReservation.CONFIRMEE), any(), any(), anyLong())).thenReturn(resultat);
    }

    // ---------- Création : cas nominal ----------

    @Test
    @DisplayName("Création valide : EN_ATTENTE, rattachée à l'utilisateur connecté, prix = durée x tarif")
    void creer_ok() {
        stubSalleEtUser();
        stubConflits(1L, 0);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse res = service.create(request(1L, debut, debut.plusHours(2), 30), EMAIL);

        assertThat(res.statut()).isEqualTo(StatutReservation.EN_ATTENTE);
        assertThat(res.prixTotal()).isEqualByComparingTo("160.00");   // 2 h x 80
        assertThat(res.userId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Prix : 1 h 30 x 80 = 120")
    void prix_unHeureTrenteMinutes() {
        stubSalleEtUser();
        stubConflits(1L, 0);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse res = service.create(request(1L, debut, debut.plusMinutes(90), 10), EMAIL);

        assertThat(res.prixTotal()).isEqualByComparingTo("120.00");
    }

    // ---------- Règle 2 : conflit ----------

    @Test
    @DisplayName("Conflit détecté : exception 'Salle déjà réservée sur ce créneau'")
    void creer_conflit() {
        stubSalleEtUser();
        stubConflits(1L, 1);

        assertThatThrownBy(() -> service.create(request(1L, debut, debut.plusHours(2), 30), EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Salle déjà réservée sur ce créneau");
    }

    @Test
    @DisplayName("Salle différente : la vérification porte sur la salle demandée")
    void creer_salleDifferente() {
        Salle autreSalle = new Salle();
        autreSalle.setId(2L);
        autreSalle.setNom("Salle Utique");
        autreSalle.setCapacite(50);
        autreSalle.setTarifHoraire(new BigDecimal("100.00"));
        autreSalle.setActive(true);

        when(salleRepository.findById(2L)).thenReturn(Optional.of(autreSalle));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        stubConflits(2L, 0);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse res = service.create(request(2L, debut, debut.plusHours(2), 30), EMAIL);

        assertThat(res.salleId()).isEqualTo(2L);
        verify(reservationRepository).countConflits(
                eq(2L), eq(StatutReservation.CONFIRMEE), eq(debut), eq(debut.plusHours(2)), anyLong());
    }

    // ---------- Règles 1, 3, 4 et créneaux ----------

    @Test
    @DisplayName("Règle 1 : dateFin avant dateDebut")
    void creer_finAvantDebut() {
        stubSalleEtUser();
        assertThatThrownBy(() -> service.create(request(1L, debut, debut.minusHours(1), 30), EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("postérieure");
    }

    @Test
    @DisplayName("Règle 1 : dateFin égale à dateDebut")
    void creer_finEgaleDebut() {
        stubSalleEtUser();
        assertThatThrownBy(() -> service.create(request(1L, debut, debut, 30), EMAIL))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Règle 1 : dateDebut dans le passé")
    void creer_passe() {
        stubSalleEtUser();
        LocalDateTime passe = LocalDateTime.now().minusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);
        assertThatThrownBy(() -> service.create(request(1L, passe, passe.plusHours(2), 30), EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("passé");
    }

    @Test
    @DisplayName("Règle 3 : capacité dépassée")
    void creer_capaciteDepassee() {
        stubSalleEtUser();
        assertThatThrownBy(() -> service.create(request(1L, debut, debut.plusHours(2), 60), EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("capacité");
    }

    @Test
    @DisplayName("Règle 4 : salle désactivée")
    void creer_salleDesactivee() {
        salle.setActive(false);
        stubSalleEtUser();
        assertThatThrownBy(() -> service.create(request(1L, debut, debut.plusHours(2), 30), EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("désactivée");
    }

    @Test
    @DisplayName("Pas de 30 minutes : 09:15 refusé")
    void creer_pasDeTrenteMinutes() {
        stubSalleEtUser();
        LocalDateTime d = debut.plusMinutes(15);
        assertThatThrownBy(() -> service.create(request(1L, d, d.plusHours(2), 30), EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("30 minutes");
    }

    @Test
    @DisplayName("Durée minimale : 30 minutes refusées")
    void creer_dureeTropCourte() {
        stubSalleEtUser();
        assertThatThrownBy(() -> service.create(request(1L, debut, debut.plusMinutes(30), 30), EMAIL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1 heure");
    }

    // ---------- Règle 5 : workflow des statuts ----------

    private Reservation reservation(StatutReservation statut, User proprietaire) {
        Reservation r = new Reservation();
        r.setId(10L);
        r.setSalle(salle);
        r.setUser(proprietaire);
        r.setTitre("Formation");
        r.setType(TypeReservation.FORMATION);
        r.setDateDebut(debut);
        r.setDateFin(debut.plusHours(2));
        r.setNbParticipants(30);
        r.setPrixTotal(new BigDecimal("160.00"));
        r.setStatut(statut);
        return r;
    }

    private Reservation reservation(StatutReservation statut) {
        return reservation(statut, user);
    }

    @Test
    @DisplayName("Confirmation : revérifie le conflit et passe à CONFIRMEE")
    void confirmer_ok() {
        Reservation r = reservation(StatutReservation.EN_ATTENTE);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(r));
        stubConflits(1L, 0);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.confirmer(10L).statut()).isEqualTo(StatutReservation.CONFIRMEE);
    }

    @Test
    @DisplayName("Confirmation refusée si un conflit est apparu entre-temps")
    void confirmer_conflit() {
        Reservation r = reservation(StatutReservation.EN_ATTENTE);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(r));
        stubConflits(1L, 1);

        assertThatThrownBy(() -> service.confirmer(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Salle déjà réservée sur ce créneau");
        assertThat(r.getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
    }

    @Test
    @DisplayName("On ne confirme pas une demande déjà refusée")
    void confirmer_statutInvalide() {
        when(reservationRepository.findById(10L))
                .thenReturn(Optional.of(reservation(StatutReservation.REFUSEE)));

        assertThatThrownBy(() -> service.confirmer(10L)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Refus d'une demande en attente")
    void refuser_ok() {
        when(reservationRepository.findById(10L))
                .thenReturn(Optional.of(reservation(StatutReservation.EN_ATTENTE)));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.refuser(10L).statut()).isEqualTo(StatutReservation.REFUSEE);
    }

    // ---------- Annulation et droits ----------

    @Test
    @DisplayName("Le client annule sa réservation confirmée avant le début")
    void annuler_proprietaire() {
        when(reservationRepository.findById(10L))
                .thenReturn(Optional.of(reservation(StatutReservation.CONFIRMEE)));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.annuler(10L, EMAIL, false).statut()).isEqualTo(StatutReservation.ANNULEE);
    }

    @Test
    @DisplayName("On n'annule pas une réservation déjà refusée")
    void annuler_statutInvalide() {
        when(reservationRepository.findById(10L))
                .thenReturn(Optional.of(reservation(StatutReservation.REFUSEE)));

        assertThatThrownBy(() -> service.annuler(10L, EMAIL, false)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Un client ne peut pas annuler la réservation d'un autre client")
    void annuler_autreClient() {
        when(reservationRepository.findById(10L))
                .thenReturn(Optional.of(reservation(StatutReservation.CONFIRMEE, autre)));

        assertThatThrownBy(() -> service.annuler(10L, EMAIL, false))
                .isInstanceOf(ForbiddenException.class);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Un client ne peut plus annuler une réservation déjà commencée")
    void annuler_clientApresDebut() {
        Reservation r = reservation(StatutReservation.CONFIRMEE);
        r.setDateDebut(LocalDateTime.now().minusHours(1));
        r.setDateFin(LocalDateTime.now().plusHours(1));
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.annuler(10L, EMAIL, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("commencée");
    }

    @Test
    @DisplayName("L'admin peut annuler la réservation de n'importe quel client")
    void annuler_adminAutreClient() {
        when(reservationRepository.findById(10L))
                .thenReturn(Optional.of(reservation(StatutReservation.CONFIRMEE, autre)));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.annuler(10L, ADMIN_EMAIL, true).statut()).isEqualTo(StatutReservation.ANNULEE);
    }

    @Test
    @DisplayName("Lecture : le propriétaire accède à sa réservation")
    void lire_proprietaire() {
        when(reservationRepository.findById(10L))
                .thenReturn(Optional.of(reservation(StatutReservation.CONFIRMEE)));

        assertThat(service.findById(10L, EMAIL, false).id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Lecture : un client ne lit pas la réservation d'un autre")
    void lire_autreClient() {
        when(reservationRepository.findById(10L))
                .thenReturn(Optional.of(reservation(StatutReservation.CONFIRMEE, autre)));

        assertThatThrownBy(() -> service.findById(10L, EMAIL, false))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Lecture : l'admin lit n'importe quelle réservation")
    void lire_admin() {
        when(reservationRepository.findById(10L))
                .thenReturn(Optional.of(reservation(StatutReservation.CONFIRMEE, autre)));

        assertThat(service.findById(10L, ADMIN_EMAIL, true).id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Modification : un client ne modifie pas la réservation d'un autre")
    void modifier_autreClient() {
        when(reservationRepository.findById(10L))
                .thenReturn(Optional.of(reservation(StatutReservation.EN_ATTENTE, autre)));

        assertThatThrownBy(() -> service.update(10L, request(1L, debut, debut.plusHours(2), 30), EMAIL, false))
                .isInstanceOf(ForbiddenException.class);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    // ---------- Calendrier ----------

    private List<Reservation> troisReservations() {
        Reservation miene = reservation(StatutReservation.CONFIRMEE, user);
        miene.setId(10L);
        Reservation autreConfirmee = reservation(StatutReservation.CONFIRMEE, autre);
        autreConfirmee.setId(11L);
        Reservation autreEnAttente = reservation(StatutReservation.EN_ATTENTE, autre);
        autreEnAttente.setId(12L);
        return List.of(miene, autreConfirmee, autreEnAttente);
    }

    @Test
    @DisplayName("Calendrier client : ses réservations en détail, celles des autres masquées (confirmées seulement)")
    void calendrier_client() {
        when(reservationRepository.findByPeriode(any(), any())).thenReturn(troisReservations());

        List<CalendrierEvent> events = service.findCalendrier(debut.minusDays(1), debut.plusDays(1), EMAIL, false);

        assertThat(events).hasSize(2);   // la demande en attente d'un autre client n'apparaît pas

        CalendrierEvent sien = events.stream().filter(CalendrierEvent::mine).findFirst().orElseThrow();
        assertThat(sien.titre()).isEqualTo("Formation");
        assertThat(sien.id()).isEqualTo(10L);

        CalendrierEvent masque = events.stream().filter(e -> !e.mine()).findFirst().orElseThrow();
        assertThat(masque.titre()).isEqualTo("Occupé");
        assertThat(masque.clientNom()).isNull();
        assertThat(masque.id()).isNull();
        assertThat(masque.statut()).isEqualTo(StatutReservation.CONFIRMEE);
    }

    @Test
    @DisplayName("Calendrier admin : tous les détails, tous les statuts")
    void calendrier_admin() {
        when(reservationRepository.findByPeriode(any(), any())).thenReturn(troisReservations());

        List<CalendrierEvent> events = service.findCalendrier(debut.minusDays(1), debut.plusDays(1), ADMIN_EMAIL, true);

        assertThat(events).hasSize(3);
        assertThat(events).allSatisfy(e -> {
            assertThat(e.titre()).isEqualTo("Formation");
            assertThat(e.clientNom()).isNotBlank();
        });
    }
}