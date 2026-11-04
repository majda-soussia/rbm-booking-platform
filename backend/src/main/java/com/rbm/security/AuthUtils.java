package com.rbm.security;

import org.springframework.security.core.Authentication;

public final class AuthUtils {

    private AuthUtils() {}

    public static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}