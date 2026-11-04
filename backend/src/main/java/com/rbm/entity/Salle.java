// Salle.java
package com.rbm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "salles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Salle {
    private String adresse;

    @Column(length = 500)
    private String localisationUrl;
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private Integer capacite;

    private Integer etage;

    @Column(length = 1000)
    private String description;

    private String photoUrl;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifHoraire;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany
    @JoinTable(name = "salle_equipements",
            joinColumns = @JoinColumn(name = "salle_id"),
            inverseJoinColumns = @JoinColumn(name = "equipement_id"))
    private Set<Equipement> equipements = new HashSet<>();
}