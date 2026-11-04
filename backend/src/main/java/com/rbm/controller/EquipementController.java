package com.rbm.controller;

import com.rbm.dto.EquipementDto;
import com.rbm.service.EquipementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipements")
@RequiredArgsConstructor
public class EquipementController {

    private final EquipementService service;

    @GetMapping
    public List<EquipementDto> getAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public EquipementDto getOne(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<EquipementDto> create(@Valid @RequestBody EquipementDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public EquipementDto update(@PathVariable Long id, @Valid @RequestBody EquipementDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}