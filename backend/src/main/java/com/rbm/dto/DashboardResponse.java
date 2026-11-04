package com.rbm.dto;

import java.math.BigDecimal;

public record DashboardResponse(
        long totalSalles,
        long sallesActives,
        long totalClients,
        long reservationsEnAttente,
        long reservationsConfirmees,
        long reservationsRefusees,
        long reservationsAnnulees,
        long confirmeesAVenir,
        BigDecimal chiffreAffaires) {}