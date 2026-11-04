package com.rbm.service;

import com.rbm.dto.EquipementDto;
import com.rbm.entity.Equipement;
import com.rbm.exception.ResourceNotFoundException;
import com.rbm.repository.EquipementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipementService {

    private final EquipementRepository repository;

    public List<EquipementDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public EquipementDto findById(Long id) {
        return toDto(getEntity(id));
    }

    public EquipementDto create(EquipementDto dto) {
        Equipement e = new Equipement();
        e.setLibelle(dto.libelle());
        e.setDescription(dto.description());
        return toDto(repository.save(e));
    }

    public EquipementDto update(Long id, EquipementDto dto) {
        Equipement e = getEntity(id);
        e.setLibelle(dto.libelle());
        e.setDescription(dto.description());
        return toDto(repository.save(e));
    }

    public void delete(Long id) {
        repository.delete(getEntity(id));
    }

    private Equipement getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Équipement introuvable : " + id));
    }

    private EquipementDto toDto(Equipement e) {
        return new EquipementDto(e.getId(), e.getLibelle(), e.getDescription());
    }
}