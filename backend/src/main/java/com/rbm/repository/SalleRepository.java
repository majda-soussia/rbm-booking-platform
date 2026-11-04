package com.rbm.repository;

import com.rbm.entity.Salle;
import com.rbm.entity.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SalleRepository extends JpaRepository<Salle, Long> {

    @Query("""
            SELECT s FROM Salle s
            WHERE s.active = true
              AND NOT EXISTS (
                  SELECT r.id FROM Reservation r
                  WHERE r.salle = s
                    AND r.statut = :statut
                    AND :debut < r.dateFin
                    AND :fin > r.dateDebut)
            """)
    List<Salle> findDisponibles(@Param("debut") LocalDateTime debut,
                                @Param("fin") LocalDateTime fin,
                                @Param("statut") StatutReservation statut);

    // Compteur du tableau de bord
    long countByActiveTrue();
}