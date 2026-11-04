// dto/AuthResponse.java
package com.rbm.dto;

public record AuthResponse(
        String token,
        String type,
        long expiresInMs,
        UserResponse user) {}