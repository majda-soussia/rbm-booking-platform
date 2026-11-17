package com.rbm.dto;
public record RegisterResponse(String message, boolean verificationRequise, AuthResponse session) {}