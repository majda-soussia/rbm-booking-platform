// dto/CalendrierEvent.java
package com.rbm.dto;

import com.rbm.entity.StatutReservation;
import java.time.LocalDateTime;

public record CalendrierEvent(
        Long id,                 // null pour un créneau masqué
        Long salleId,
        String salleNom,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        StatutReservation statut,
        String titre,            // "Occupé" pour un créneau masqué
        String clientNom,        // null pour un créneau masqué
        boolean mine) {}