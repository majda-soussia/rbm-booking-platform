package com.rbm.config;

import com.rbm.entity.*;
import com.rbm.repository.EquipementRepository;
import com.rbm.repository.RoleRepository;
import com.rbm.repository.SalleRepository;
import com.rbm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Données de démonstration : actif seulement si app.demo-data=true, et seulement sur une base sans salle. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
public class DemoDataInitializer implements CommandLineRunner {

    private final EquipementRepository equipementRepository;
    private final SalleRepository salleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo-client-password:}")
    private String clientPassword;

    @Override
    public void run(String... args) {
        if (salleRepository.count() > 0) {
            return;   // la base contient déjà des salles : on ne touche à rien
        }

        Equipement projecteur = equipement("Projecteur", "Full HD, HDMI et VGA");
        Equipement visio = equipement("Visioconférence", "Caméra HD et micro d'ambiance");
        Equipement tableau = equipement("Tableau blanc", "Tableau blanc magnétique");
        Equipement clim = equipement("Climatisation", "Climatisation réglable");

        salle("Salle Carthage", 50, 1, "Grande salle de séminaire lumineuse", "Avenue Habib Bourguiba, Tunis",
                "80.00", Set.of(projecteur, visio, clim));
        salle("Salle Utique", 20, 2, "Salle de formation équipée", "Avenue Habib Bourguiba, Tunis",
                "50.00", Set.of(projecteur, tableau));
        salle("Salle Dougga", 120, 0, "Salle de réception pour événements et mariages", "Rue de Marseille, Tunis",
                "150.00", Set.of(visio, clim));

        if (clientPassword != null && clientPassword.length() >= 8
                && !userRepository.existsByEmail("client.demo@rbm.com")) {
            Role role = roleRepository.findByLibelle(ERole.CLIENT)
                    .orElseGet(() -> roleRepository.save(new Role(null, ERole.CLIENT)));
            User client = new User();
            client.setEmail("client.demo@rbm.com");
            client.setPassword(passwordEncoder.encode(clientPassword));
            client.setNom("Démo");
            client.setPrenom("Client");
            client.setRoles(new HashSet<>(List.of(role)));
            userRepository.save(client);
        }
    }

    private Equipement equipement(String libelle, String description) {
        Equipement e = new Equipement();
        e.setLibelle(libelle);
        e.setDescription(description);
        return equipementRepository.save(e);
    }

    private void salle(String nom, int capacite, int etage, String description, String adresse,
                       String tarif, Set<Equipement> equipements) {
        Salle s = new Salle();
        s.setNom(nom);
        s.setCapacite(capacite);
        s.setEtage(etage);
        s.setDescription(description);
        s.setAdresse(adresse);
        s.setTarifHoraire(new BigDecimal(tarif));
        s.setActive(true);
        s.setEquipements(new HashSet<>(equipements));
        salleRepository.save(s);
    }
}