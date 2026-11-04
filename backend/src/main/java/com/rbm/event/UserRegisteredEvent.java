package com.rbm.event;

import java.time.LocalDateTime;

public record UserRegisteredEvent(
        String email,
        String nom,
        String prenom,
        String tel,
        LocalDateTime dateInscription) {}