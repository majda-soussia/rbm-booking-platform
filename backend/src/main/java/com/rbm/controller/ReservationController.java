package com.rbm.controller;

import com.rbm.dto.CalendrierEvent;
import com.rbm.dto.ReservationRequest;
import com.rbm.dto.ReservationResponse;
import com.rbm.security.AuthUtils;
import com.rbm.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService service;

    @GetMapping   // admin uniquement (SecurityConfig)
    public List<ReservationResponse> getAll() { return service.findAll(); }

    @GetMapping("/mes")
    public List<ReservationResponse> mesReservations(Authentication auth) {
        return service.findMine(auth.getName());
    }

    @GetMapping("/{id}")
    public ReservationResponse getOne(@PathVariable Long id, Authentication auth) {
        return service.findById(id, auth.getName(), AuthUtils.isAdmin(auth));
    }

    @GetMapping("/calendrier")
    public List<CalendrierEvent> calendrier(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            Authentication auth) {
        return service.findCalendrier(debut, fin, auth.getName(), AuthUtils.isAdmin(auth));
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest req,
                                                      Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, auth.getName()));
    }

    @PutMapping("/{id}")
    public ReservationResponse update(@PathVariable Long id, @Valid @RequestBody ReservationRequest req,
                                      Authentication auth) {
        return service.update(id, req, auth.getName(), AuthUtils.isAdmin(auth));
    }

    @PutMapping("/{id}/confirmer")   // admin uniquement
    public ReservationResponse confirmer(@PathVariable Long id) { return service.confirmer(id); }

    @PutMapping("/{id}/refuser")     // admin uniquement
    public ReservationResponse refuser(@PathVariable Long id) { return service.refuser(id); }

    @PutMapping("/{id}/annuler")
    public ReservationResponse annuler(@PathVariable Long id, Authentication auth) {
        return service.annuler(id, auth.getName(), AuthUtils.isAdmin(auth));
    }

    @DeleteMapping("/{id}")          // admin uniquement
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
} 