package com.rbm.repository;

import com.rbm.entity.Reservation;
import com.rbm.entity.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // Suppression protégée d'une salle
    boolean existsBySalleIdAndStatutAndDateFinAfter(Long salleId, StatutReservation statut, LocalDateTime date);
    boolean existsBySalleId(Long salleId);
    boolean existsByUserId(Long userId);

    // DÉTECTION DE CONFLIT : nouveauDebut < existantFin ET nouveauFin > existantDebut
    @Query("""
            SELECT COUNT(r) FROM Reservation r
            WHERE r.salle.id = :salleId
              AND r.statut = :statut
              AND :debut < r.dateFin
              AND :fin > r.dateDebut
              AND r.id <> :excludeId
            """)
    long countConflits(@Param("salleId") Long salleId,
                       @Param("statut") StatutReservation statut,
                       @Param("debut") LocalDateTime debut,
                       @Param("fin") LocalDateTime fin,
                       @Param("excludeId") Long excludeId);

    // Réservations qui touchent la période (calendrier)
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.dateDebut < :fin AND r.dateFin > :debut
            ORDER BY r.dateDebut
            """)
    List<Reservation> findByPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    // "Mes réservations" : par l'email de l'utilisateur connecté
    List<Reservation> findByUserEmailOrderByDateDebutDesc(String email);

    // Compteurs du tableau de bord (étape 7)
    long countByStatut(StatutReservation statut);

    long countByStatutAndDateDebutAfter(StatutReservation statut, LocalDateTime date);

    @Query("SELECT COALESCE(SUM(r.prixTotal), 0) FROM Reservation r WHERE r.statut = :statut")
    BigDecimal sumPrixTotalByStatut(@Param("statut") StatutReservation statut);
}