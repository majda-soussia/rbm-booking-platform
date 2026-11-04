// config/DataInitializer.java
package com.rbm.config;

import com.rbm.entity.ERole;
import com.rbm.entity.Role;
import com.rbm.entity.User;
import com.rbm.repository.RoleRepository;
import com.rbm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@rbm.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin123!}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        for (ERole r : ERole.values()) {
            if (roleRepository.findByLibelle(r).isEmpty()) {
                Role role = new Role();
                role.setLibelle(r);
                roleRepository.save(role);
            }
        }

        if (!userRepository.existsByEmail(adminEmail)) {
            Role adminRole = roleRepository.findByLibelle(ERole.ADMIN).orElseThrow();
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setNom("Admin");
            admin.setPrenom("RBM");
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
        }
    }
}