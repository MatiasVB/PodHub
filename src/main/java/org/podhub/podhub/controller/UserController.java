package org.podhub.podhub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.dto.UserPatchRequest;
import org.podhub.podhub.exception.BadRequestException;
import org.podhub.podhub.exception.ResourceNotFoundException;
import org.podhub.podhub.model.Role;
import org.podhub.podhub.model.User;
import org.podhub.podhub.model.enums.UserRole;
import org.podhub.podhub.model.enums.UserStatus;
import org.podhub.podhub.repository.RoleRepository;
import org.podhub.podhub.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Tag(name = "Users", description = "User management endpoints")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleRepository roleRepository;

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
     * PATCH /api/users/{id}
     * Partially update a user profile
     * Regular users can update their own profile (displayName, avatarUrl, bio)
     * Admins can update any user including status field
     */
    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Partially update user",
               description = "Updates only the provided fields. Regular users can only update their own profile.")
    public ResponseEntity<User> patchUser(
            @PathVariable String id,
            @Valid @RequestBody UserPatchRequest patchRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        User currentUser = userService.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        User patched = userService.patchUser(id, patchRequest, currentUser.getId(), isAdmin);
        return ResponseEntity.ok(patched);
    }

    /**
     * DELETE /api/users/{id}
     * Elimina un usuario por ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/users?cursor={timestamp}&limit={number}&name={query}&role={roleOrId}&status={status}
     * Lista usuarios con filtros opcionales y paginación cursor-based
     *
     * Ejemplos:
     * - GET /api/users?limit=20                        (todos los usuarios)
     * - GET /api/users?name=john                       (búsqueda por nombre)
     * - GET /api/users?role=USER                       (por rol nombre - convierte a roleId)
     * - GET /api/users?status=ACTIVE                   (por estado)
     * - GET /api/users?cursor={timestamp}&limit=20     (siguiente página)
     *
     * Nota: El parámetro 'role' acepta nombre de rol (USER, CREATOR, ADMIN) y lo convierte a roleId internamente
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<User>> getAllUsers(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;

        // Si se proporciona role, convertir de nombre a roleId
        String roleId = null;
        if (role != null && !role.trim().isEmpty()) {
            try {
                UserRole roleEnum = UserRole.valueOf(role.toUpperCase());
                Role roleEntity = roleRepository.findByName(roleEnum)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleEnum));
                roleId = roleEntity.getId();
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Valor de 'role' inválido: " + role);
            }
        }

        PaginatedResponse<User> response = userService.findAll(cursorInstant, limit, name, roleId, status);
        return ResponseEntity.ok(response);
    }
}
