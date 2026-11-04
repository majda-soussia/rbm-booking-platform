package com.rbm.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Set;

public record SalleRequest(
        @NotBlank(message = "Le nom est obligatoire") String nom,
        @NotNull(message = "La capacité est obligatoire")
        @Min(value = 1, message = "La capacité doit être au moins 1") Integer capacite,
        Integer etage,
        String description,
        String adresse,
        @Pattern(regexp = "^$|^https?://.+", message = "Le lien de localisation doit commencer par http:// ou https://")
        @Size(max = 500, message = "Le lien est trop long (500 caractères maximum)") String localisationUrl,
        @NotNull(message = "Le tarif horaire est obligatoire")
        @DecimalMin(value = "0.0", inclusive = false, message = "Le tarif doit être positif") BigDecimal tarifHoraire,
        Boolean active,
        Set<Long> equipementIds) {}