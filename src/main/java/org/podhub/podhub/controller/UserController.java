package org.podhub.podhub.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.exception.BadRequestException;
import org.podhub.podhub.model.User;
import org.podhub.podhub.model.enums.UserRole;
import org.podhub.podhub.model.enums.UserStatus;
import org.podhub.podhub.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * POST /api/users
     * Crea un nuevo usuario
     */
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        User created = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/users/{id}
     * Obtiene un usuario por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/users/{id}
     * Actualiza un usuario existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable String id,
            @Valid @RequestBody User user) {
        try {
            User updated = userService.updateUser(id, user);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/users/{id}
     * Elimina un usuario por ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/users?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista todos los usuarios con paginación cursor-based
     *
     * Primera página: GET /api/users?limit=20
     * Siguiente página: GET /api/users?cursor={nextCursor}&limit=20
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<User>> getAllUsers(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<User> response = userService.findAll(cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/users/search?name=name&cursor=2024-01-15T10:30:00Z&limit=20
     * Busca usuarios por nombre con paginación cursor-based
     */
    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<User>> searchUsers(
            @RequestParam String name,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<User> response = userService.searchByName(name, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/users/role/{role}?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista usuarios por rol con paginación cursor-based
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<PaginatedResponse<User>> getUsersByRole(
            @PathVariable String role,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;

        // Convertimos el String a enum y si falla, delegamos en tu GlobalExceptionHandler (400 Bad Request)
        final UserRole roleEnum;
        try {
            roleEnum = UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Valor de 'role' inválido: " + role);
        }

        PaginatedResponse<User> response = userService.findByRole(roleEnum, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/users/status/{status}?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista usuarios por estado con paginación cursor-based
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<PaginatedResponse<User>> getUsersByStatus(
            @PathVariable String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;

        // Convertimos el String a enum y lanzamos BadRequestException si no existe
        final UserStatus statusEnum;
        try {
            statusEnum = UserStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Valor de 'status' inválido: " + status);
        }

        PaginatedResponse<User> response = userService.findByStatus(statusEnum, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }
}
