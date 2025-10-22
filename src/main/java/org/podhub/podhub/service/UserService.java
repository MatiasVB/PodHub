package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.dto.PaginatedResponse;
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
            throw new IllegalArgumentException("Email is required");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        User saved = userRepository.save(user);
        log.info("User created {}", saved.getId());
        return saved;
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Obtiene todos los usuarios con paginación cursor-based
     *
     * @param cursor Timestamp del último elemento (null para primera página)
     * @param limit  Número máximo de elementos a retornar
     * @return Respuesta paginada con cursor para siguiente página
     */
    public PaginatedResponse<User> findAll(Instant cursor, int limit) {
        log.debug("Finding all users with cursor: {} and limit: {}", cursor, limit);

        // Agregar 1 al límite para detectar si hay más elementos
        List<User> users;
        if (cursor == null) {
            users = userRepository.findFirstUsers(limit + 1);
        } else {
            users = userRepository.findNextUsers(cursor, limit + 1);
        }

        return buildPaginatedResponse(users, limit);
    }

    /**
     * Obtiene los usuarios por estado con paginación cursor-based
     */
    public PaginatedResponse<User> findByStatus(UserStatus status, Instant cursor, int limit) {
        log.debug("Finding users by status: {} with cursor: {} and limit: {}", status, cursor, limit);

        List<User> users;
        if (cursor == null) {
            users = userRepository.findFirstUsersByStatus(status, limit + 1);
        } else {
            users = userRepository.findNextUsersByStatus(status, cursor, limit + 1);
        }

        return buildPaginatedResponse(users, limit);
    }

    /**
     * Obtiene los usuarios por rol con paginación cursor-based
     */
    public PaginatedResponse<User> findByRole(UserRole role, Instant cursor, int limit) {
        log.debug("Finding users by role: {} with cursor: {} and limit: {}", role, cursor, limit);

        List<User> users;
        if (cursor == null) {
            users = userRepository.findFirstUsersByRole(role, limit + 1);
        } else {
            users = userRepository.findNextUsersByRole(role, cursor, limit + 1);
        }

        return buildPaginatedResponse(users, limit);
    }

    /**
     * Busca usuarios por nombre con paginación cursor-based
     */
    public PaginatedResponse<User> searchByName(String name, Instant cursor, int limit) {
        log.debug("Searching users by name: {} with cursor: {} and limit: {}", name, cursor, limit);

        List<User> users;
        if (cursor == null) {
            users = userRepository.findFirstUsersByName(name, limit + 1);
        } else {
            users = userRepository.findNextUsersByName(name, cursor, limit + 1);
        }

        return buildPaginatedResponse(users, limit);
    }

    public User updateUser(String id, User updated) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        // Si cambia el email, valida unicidad
        if (updated.getEmail() != null && !updated.getEmail().equalsIgnoreCase(existing.getEmail())) {
            if (userRepository.existsByEmail(updated.getEmail())) {
                throw new IllegalArgumentException("Email already in use");
            }
        }

        updated.setId(id);
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setUpdatedAt(Instant.now());
        User saved = userRepository.save(updated);
        log.info("User updated {}", id);
        return saved;
    }

    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        userRepository.deleteById(id);
        log.info("User deleted {}", id);
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
