// SalleController.java
package com.rbm.controller;

import com.rbm.dto.SalleRequest;
import com.rbm.dto.SalleResponse;
import com.rbm.service.SalleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
@RestController
@RequestMapping("/api/salles")
@RequiredArgsConstructor
public class SalleController {

    private final SalleService service;

    @GetMapping
    public List<SalleResponse> getAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public SalleResponse getOne(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<SalleResponse> create(@Valid @RequestBody SalleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public SalleResponse update(@PathVariable Long id, @Valid @RequestBody SalleRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/active")
    public SalleResponse setActive(@PathVariable Long id, @RequestParam boolean value) {
        return service.setActive(id, value);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/disponibles")
    public List<SalleResponse> disponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(required = false) Integer capacite,
            @RequestParam(required = false) Set<Long> equipementIds) {
        return service.findDisponibles(debut, fin, capacite, equipementIds);
    }
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SalleResponse uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return service.updatePhoto(id, file);
    }
}