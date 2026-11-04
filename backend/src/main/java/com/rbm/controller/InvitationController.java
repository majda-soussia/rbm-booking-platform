package com.rbm.controller;

import com.rbm.dto.InvitationResponse;
import com.rbm.dto.InvitationsRequest;
import com.rbm.security.AuthUtils;
import com.rbm.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations/{reservationId}/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService service;

    @GetMapping
    public List<InvitationResponse> historique(@PathVariable Long reservationId, Authentication auth) {
        return service.findByReservation(reservationId, auth.getName(), AuthUtils.isAdmin(auth));
    }

    @PostMapping
    public ResponseEntity<List<InvitationResponse>> inviter(@PathVariable Long reservationId,
                                                            @Valid @RequestBody InvitationsRequest req,
                                                            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.inviter(reservationId, req, auth.getName(), AuthUtils.isAdmin(auth)));
    }
}