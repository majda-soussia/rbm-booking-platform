package com.rbm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record InvitationsRequest(
        @NotEmpty(message = "La liste des invités est vide")
        List<@Valid InviteRequest> invites) {}