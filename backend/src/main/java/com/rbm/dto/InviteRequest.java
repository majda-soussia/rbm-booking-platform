package com.rbm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteRequest(
        @NotBlank(message = "L'email de l'invité est obligatoire")
        @Email(message = "Format d'email invalide") String email,
        String nom) {}