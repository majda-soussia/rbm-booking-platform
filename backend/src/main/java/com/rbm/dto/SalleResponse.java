package com.rbm.dto;

import java.math.BigDecimal;
import java.util.List;

public record SalleResponse(
        Long id,
        String nom,
        Integer capacite,
        Integer etage,
        String description,
        String adresse,
        String localisationUrl,
        String photoUrl,
        BigDecimal tarifHoraire,
        boolean active,
        List<EquipementDto> equipements) {}