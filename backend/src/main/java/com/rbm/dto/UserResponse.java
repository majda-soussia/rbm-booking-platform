// UserResponse.java
package com.rbm.dto;

import com.rbm.entity.ERole;
import java.util.Set;

public record UserResponse(
        Long id,
        String email,
        String tel,
        String nom,
        String prenom,
        Set<ERole> roles) {}