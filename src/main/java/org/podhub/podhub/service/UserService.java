package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.exception.BadRequestException;
import org.podhub.podhub.exception.ConflictException;
import org.podhub.podhub.exception.ResourceNotFoundException;
import org.podhub.podhub.model.User;
import org.podhub.podhub.model.enums.UserRole;
import org.podhub.podhub.model.enums.UserStatus;
import org.podhub.podhub.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(User user) {
        log.debug("Creating user with email {}", user.getEmail());
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ConflictException("Email already in use: " + user.getEmail());
        }
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        User saved = userRepository.save(user);
        log.info("User created successfully with id: {}", saved.getId());
        return saved;
    }

    /**
     * Obtiene un usuario por id
     */
    public Optional<User> findById(String id) {
        log.debug("Finding user by id: {}", id);
        return userRepository.findById(id);
    }

    /**
     * Obtiene un usuario por email
     */
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Obtiene todos los usuarios con paginación cursor-based y filtros opcionales
     *
     * @param cursor Timestamp del último elemento (null para primera página)
     * @param limit Número máximo de elementos a retornar
     * @param name Filtro opcional por búsqueda en nombre
     * @param roleId Filtro opcional por rol
     * @param status Filtro opcional por estado
     * @return Respuesta paginada con cursor para siguiente página
     */
    public PaginatedResponse<User> findAll(Instant cursor, int limit, String name, String roleId, String status) {
        log.debug("Finding users with cursor: {}, limit: {}, name: {}, roleId: {}, status: {}",
                  cursor, limit, name, roleId, status);

        List<User> users;

        // Determinar qué método del repository usar según los filtros
        if (name != null && !name.trim().isEmpty()) {
            // Búsqueda por nombre tiene prioridad
            if (cursor == null) {
                users = userRepository.findFirstUsersByName(name, limit + 1);
            } else {
                users = userRepository.findNextUsersByName(name, cursor, limit + 1);
            }
        } else if (roleId != null && !roleId.trim().isEmpty()) {
            // Filtro por rol
            if (cursor == null) {
                users = userRepository.findFirstUsersByRoleId(roleId, limit + 1);
            } else {
                users = userRepository.findNextUsersByRoleId(roleId, cursor, limit + 1);
            }
        } else if (status != null && !status.trim().isEmpty()) {
            // Filtro por estado
            UserStatus userStatus = UserStatus.valueOf(status.toUpperCase());
            if (cursor == null) {
                users = userRepository.findFirstUsersByStatus(userStatus, limit + 1);
            } else {
                users = userRepository.findNextUsersByStatus(userStatus, cursor, limit + 1);
            }
        } else {
            // Sin filtros, todos los usuarios
            if (cursor == null) {
                users = userRepository.findFirstUsers(limit + 1);
            } else {
                users = userRepository.findNextUsers(cursor, limit + 1);
            }
        }

        return buildPaginatedResponse(users, limit);
    }

    /**
     * @deprecated Use findAll(cursor, limit, name, roleId, status) instead
     */
    @Deprecated
    public PaginatedResponse<User> findByStatus(UserStatus status, Instant cursor, int limit) {
        return findAll(cursor, limit, null, null, status.name());
    }

    /**
     * @deprecated Use findAll(cursor, limit, name, roleId, status) instead
     */
    @Deprecated
    public PaginatedResponse<User> findByRoleId(String roleId, Instant cursor, int limit) {
        return findAll(cursor, limit, null, roleId, null);
    }

    /**
     * @deprecated Use findAll(cursor, limit, name, roleId, status) instead
     */
    @Deprecated
    public PaginatedResponse<User> searchByName(String name, Instant cursor, int limit) {
        return findAll(cursor, limit, name, null, null);
    }

    public User updateUser(String id, User updated) {
        log.debug("Updating user with id: {}", id);

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (updated.getEmail() == null || updated.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (!updated.getEmail().equalsIgnoreCase(existing.getEmail())
                && userRepository.existsByEmail(updated.getEmail())) {
            throw new ConflictException("Email already in use: " + updated.getEmail());
        }

        updated.setId(id);
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setUpdatedAt(Instant.now());

        User saved = userRepository.save(updated);
        log.info("User updated successfully with id: {}", saved.getId());
        return saved;
    }

    public void deleteUser(String id) {
        log.debug("Deleting user with id: {}", id);
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
        log.info("User deleted successfully with id: {}", id);
    }

    /**
     * Construye la respuesta paginada a partir de una lista de usuarios
     *
     * @param users Lista con limit+1 elementos
     * @param limit Límite real solicitado
     * @return PaginatedResponse con nextCursor si hay más elementos
     */
    private PaginatedResponse<User> buildPaginatedResponse(List<User> users, int limit) {
        boolean hasMore = users.size() > limit;

        // Si hay más elementos, solo retornamos los primeros 'limit'
        List<User> data;
        if (hasMore) {
            data = users.subList(0, limit);
        } else {
            data = users;
        }

        // Calcular el nextCursor (createdAt del último elemento)
        String nextCursor = null;
        if (hasMore) {
            if (!data.isEmpty()) {
                nextCursor = data.get(data.size() - 1).getCreatedAt().toString();
            }
        }

        return PaginatedResponse.<User>builder()
                .data(data)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .count(data.size())
                .build();
    }
}
