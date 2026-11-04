package com.rbm.repository;

import com.rbm.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReservationRepositoryTest {

    @Autowired ReservationRepository reservationRepository;
    @Autowired SalleRepository salleRepository;
    @Autowired UserRepository userRepository;

    private Salle salle1;
    private Salle salle2;
    private User user;
    private LocalDateTime base;   // 09:00 dans le futur

    @BeforeEach
    void setUp() {
        salle1 = salleRepository.save(nouvelleSalle("Salle 1"));
        salle2 = salleRepository.save(nouvelleSalle("Salle 2"));

        User u = new User();
        u.setEmail("client@test.com");
        u.setPassword("x");
        u.setNom("Ben Ali");
        u.setPrenom("Sami");
        user = userRepository.save(u);

        base = LocalDateTime.now().plusDays(10).withHour(9).withMinute(0).withSecond(0).withNano(0);

        // Réservation CONFIRMEE existante : 09:00 -> 11:00 sur la salle 1
        reservationRepository.save(reservation(salle1, StatutReservation.CONFIRMEE, base, base.plusHours(2)));
    }

    private Salle nouvelleSalle(String nom) {
        Salle s = new Salle();
        s.setNom(nom);
        s.setCapacite(50);
        s.setTarifHoraire(new BigDecimal("80.00"));
        s.setActive(true);
        return s;
    }

    private Reservation reservation(Salle salle, StatutReservation statut, LocalDateTime d, LocalDateTime f) {
        Reservation r = new Reservation();
        r.setTitre("Test");
        r.setType(TypeReservation.REUNION);
        r.setDateDebut(d);
        r.setDateFin(f);
        r.setNbParticipants(10);
        r.setStatut(statut);
        r.setPrixTotal(new BigDecimal("160.00"));
        r.setSalle(salle);
        r.setUser(user);
        return r;
    }

    private long conflits(Salle salle, LocalDateTime d, LocalDateTime f) {
        return reservationRepository.countConflits(salle.getId(), StatutReservation.CONFIRMEE, d, f, 0L);
    }

    @Test
    @DisplayName("Chevauchement partiel (10:00-12:00 sur 09:00-11:00) : conflit")
    void chevauchementPartiel() {
        assertThat(conflits(salle1, base.plusHours(1), base.plusHours(3))).isEqualTo(1);
    }

    @Test
    @DisplayName("Chevauchement partiel au début (08:00-10:00) : conflit")
    void chevauchementPartielAvant() {
        assertThat(conflits(salle1, base.minusHours(1), base.plusHours(1))).isEqualTo(1);
    }

    @Test
    @DisplayName("Inclusion (09:30-10:30 dans 09:00-11:00) : conflit")
    void inclusion() {
        assertThat(conflits(salle1, base.plusMinutes(30), base.plusMinutes(90))).isEqualTo(1);
    }

    @Test
    @DisplayName("Créneau englobant (08:00-12:00) : conflit")
    void englobant() {
        assertThat(conflits(salle1, base.minusHours(1), base.plusHours(3))).isEqualTo(1);
    }

    @Test
    @DisplayName("Créneaux identiques : conflit")
    void identique() {
        assertThat(conflits(salle1, base, base.plusHours(2))).isEqualTo(1);
    }

    @Test
    @DisplayName("Adjacent après (11:00-12:00) : pas de conflit")
    void adjacentApres() {
        assertThat(conflits(salle1, base.plusHours(2), base.plusHours(3))).isZero();
    }

    @Test
    @DisplayName("Adjacent avant (08:00-09:00) : pas de conflit")
    void adjacentAvant() {
        assertThat(conflits(salle1, base.minusHours(1), base)).isZero();
    }

    @Test
    @DisplayName("Salle différente, même créneau : pas de conflit")
    void salleDifferente() {
        assertThat(conflits(salle2, base, base.plusHours(2))).isZero();
    }

    @Test
    @DisplayName("Une demande EN_ATTENTE ne bloque pas le créneau")
    void enAttenteNeBloquePas() {
        LocalDateTime autreJour = base.plusDays(1);
        reservationRepository.save(reservation(salle1, StatutReservation.EN_ATTENTE, autreJour, autreJour.plusHours(2)));

        assertThat(conflits(salle1, autreJour, autreJour.plusHours(2))).isZero();
    }

    @Test
    @DisplayName("Une réservation ANNULEE ne bloque pas le créneau")
    void annuleeNeBloquePas() {
        LocalDateTime autreJour = base.plusDays(2);
        reservationRepository.save(reservation(salle1, StatutReservation.ANNULEE, autreJour, autreJour.plusHours(2)));

        assertThat(conflits(salle1, autreJour, autreJour.plusHours(2))).isZero();
    }

    @Test
    @DisplayName("Une réservation ne rentre pas en conflit avec elle-même (excludeId)")
    void exclusionDeSoiMeme() {
        Reservation existante = reservationRepository.findAll().get(0);
        long n = reservationRepository.countConflits(
                salle1.getId(), StatutReservation.CONFIRMEE, base, base.plusHours(2), existante.getId());
        assertThat(n).isZero();
    }
}