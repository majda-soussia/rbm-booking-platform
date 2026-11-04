package com.rbm.dto;

import com.rbm.entity.TypeReservation;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record ReservationRequest(
        @NotBlank(message = "Le titre est obligatoire") String titre,
        String description,
        @NotNull(message = "Le type est obligatoire") TypeReservation type,
        @NotNull(message = "La date de début est obligatoire") LocalDateTime dateDebut,
        @NotNull(message = "La date de fin est obligatoire") LocalDateTime dateFin,
        @NotNull(message = "Le nombre de participants est obligatoire")
        @Min(value = 1, message = "Il faut au moins 1 participant") Integer nbParticipants,
        @NotNull(message = "La salle est obligatoire") Long salleId) {}