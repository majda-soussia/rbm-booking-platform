// RoleRepository.java
package com.rbm.repository;

import com.rbm.entity.ERole;
import com.rbm.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByLibelle(ERole libelle);
}