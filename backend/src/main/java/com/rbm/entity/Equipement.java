// Equipement.java
package com.rbm.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "equipements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Equipement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String libelle;

    private String description;
}