package com.rbm.event;

public record VerificationEmailEvent(String email, String prenom, String token) {}