// UserRequest.java
package com.rbm.dto;

import com.rbm.entity.ERole;
import jakarta.validation.constraints.*;
import java.util.Set;

public record UserRequest(
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide") String email,
        @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères") String password,
        String tel,
        @NotBlank(message = "Le nom est obligatoire") String nom,
        @NotBlank(message = "Le prénom est obligatoire") String prenom,
        Set<ERole> roles) {}