// EquipementRepository.java
package com.rbm.repository;

import com.rbm.entity.Equipement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipementRepository extends JpaRepository<Equipement, Long> {}