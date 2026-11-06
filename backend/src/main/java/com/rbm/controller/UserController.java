// UserController.java
package com.rbm.controller;

import com.rbm.dto.UserRequest;
import com.rbm.dto.UserResponse;
import com.rbm.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @GetMapping
    public List<UserResponse> getAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public UserResponse getOne(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest req, Authentication auth) {
    return service.update(id, req, auth.getName());
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
    service.delete(id, auth.getName());
    return ResponseEntity.noContent().build();
}
}