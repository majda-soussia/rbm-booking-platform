package com.rbm.repository;

import com.rbm.entity.ERole;
import com.rbm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Utilisateurs ayant un rôle donné (notification des admins)
    List<User> findByRoles_Libelle(ERole libelle);

    // Compteur du tableau de bord
    long countByRoles_Libelle(ERole libelle);
}