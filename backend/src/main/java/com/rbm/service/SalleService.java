// SalleService.java
package com.rbm.service;

import com.rbm.dto.EquipementDto;
import com.rbm.dto.SalleRequest;
import com.rbm.dto.SalleResponse;
import com.rbm.entity.Equipement;
import com.rbm.entity.Salle;
import com.rbm.entity.StatutReservation;
import com.rbm.exception.BusinessException;
import com.rbm.exception.ResourceNotFoundException;
import com.rbm.repository.EquipementRepository;
import com.rbm.repository.ReservationRepository;
import com.rbm.repository.SalleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalleService {

    private final SalleRepository salleRepository;
    private final EquipementRepository equipementRepository;
    private final ReservationRepository reservationRepository;
    private final FileStorageService fileStorageService;

    public List<SalleResponse> findAll() {
        return salleRepository.findAll().stream().map(this::toResponse).toList();
    }

    public SalleResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public SalleResponse create(SalleRequest req) {
        Salle salle = new Salle();
        apply(salle, req);
        return toResponse(salleRepository.save(salle));
    }

    @Transactional
    public SalleResponse update(Long id, SalleRequest req) {
        Salle salle = getEntity(id);
        apply(salle, req);
        return toResponse(salleRepository.save(salle));
    }

    @Transactional
    public void delete(Long id) {
        Salle salle = getEntity(id);

        // Règle 6 : réservations futures confirmées => suppression interdite
        if (reservationRepository.existsBySalleIdAndStatutAndDateFinAfter(
                id, StatutReservation.CONFIRMEE, LocalDateTime.now())) {
            throw new BusinessException(
                    "Impossible de supprimer cette salle : elle a des réservations futures confirmées. Vous pouvez seulement la désactiver.");
        }
        // Évite l'erreur de clé étrangère avec l'historique
        if (reservationRepository.existsBySalleId(id)) {
            throw new BusinessException(
                    "Impossible de supprimer cette salle : elle possède un historique de réservations. Désactivez-la plutôt.");
        }
        salleRepository.delete(salle);
        fileStorageService.delete(salle.getPhotoUrl());
    }

    @Transactional
    public SalleResponse setActive(Long id, boolean active) {
        Salle salle = getEntity(id);
        salle.setActive(active);
        return toResponse(salleRepository.save(salle));
    }

    // ---------- helpers ----------

    private void apply(Salle salle, SalleRequest req) {
        salle.setNom(req.nom());
        salle.setCapacite(req.capacite());
        salle.setEtage(req.etage());
        salle.setDescription(req.description());
        salle.setTarifHoraire(req.tarifHoraire());
        salle.setAdresse(req.adresse());
        salle.setLocalisationUrl(req.localisationUrl());
        if (req.active() != null) {
            salle.setActive(req.active());
        }

        Set<Equipement> equipements = new HashSet<>();
        if (req.equipementIds() != null) {
            for (Long eid : req.equipementIds()) {
                equipements.add(equipementRepository.findById(eid)
                        .orElseThrow(() -> new ResourceNotFoundException("Équipement introuvable : " + eid)));
            }
        }
        salle.setEquipements(equipements);
    }

    private Salle getEntity(Long id) {
        return salleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable : " + id));
    }
    @Transactional
    public SalleResponse updatePhoto(Long id, MultipartFile file) {
        Salle salle = getEntity(id);
        String anciennePhoto = salle.getPhotoUrl();
        salle.setPhotoUrl(fileStorageService.storeSallePhoto(id, file));
        Salle saved = salleRepository.save(salle);
        fileStorageService.delete(anciennePhoto);   // remplacement : l'ancienne photo est supprimée
        return toResponse(saved);
    }

    private SalleResponse toResponse(Salle s) {
        List<EquipementDto> eqs = s.getEquipements().stream()
                .map(e -> new EquipementDto(e.getId(), e.getLibelle(), e.getDescription()))
                .toList();
        return new SalleResponse(s.getId(), s.getNom(), s.getCapacite(), s.getEtage(),
        s.getDescription(), s.getAdresse(), s.getLocalisationUrl(), s.getPhotoUrl(),
        s.getTarifHoraire(), s.isActive(), eqs);
    }
    public List<SalleResponse> findDisponibles(LocalDateTime debut, LocalDateTime fin,
                                           Integer capacite, Set<Long> equipementIds) {
    if (!fin.isAfter(debut)) {
        throw new BusinessException("La date de fin doit être postérieure à la date de début");
    }
    return salleRepository.findDisponibles(debut, fin, StatutReservation.CONFIRMEE).stream()
            .filter(s -> capacite == null || s.getCapacite() >= capacite)
            .filter(s -> equipementIds == null || equipementIds.isEmpty()
                    || s.getEquipements().stream().map(Equipement::getId)
                        .collect(java.util.stream.Collectors.toSet()).containsAll(equipementIds))
            .map(this::toResponse)
            .toList();
}

}