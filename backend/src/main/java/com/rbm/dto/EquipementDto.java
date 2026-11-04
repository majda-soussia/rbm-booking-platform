package com.rbm.dto;

import jakarta.validation.constraints.NotBlank;

public record EquipementDto(
        Long id,
        @NotBlank(message = "Le libellé est obligatoire") String libelle,
        String description) {}