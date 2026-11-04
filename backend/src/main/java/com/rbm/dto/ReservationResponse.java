// dto/ReservationResponse.java
package com.rbm.dto;

import com.rbm.entity.StatutReservation;
import com.rbm.entity.TypeReservation;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        String titre,
        String description,
        TypeReservation type,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        Integer nbParticipants,
        StatutReservation statut,
        BigDecimal prixTotal,
        LocalDateTime dateCreation,
        Long salleId,
        String salleNom,
        Long userId,
        String userNom) {}