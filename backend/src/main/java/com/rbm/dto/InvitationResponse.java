package com.rbm.dto;

import com.rbm.entity.StatutEnvoi;

import java.time.LocalDateTime;

public record InvitationResponse(
        Long id,
        String emailInvite,
        String nomInvite,
        StatutEnvoi statutEnvoi,
        LocalDateTime dateEnvoi,
        Long reservationId) {}