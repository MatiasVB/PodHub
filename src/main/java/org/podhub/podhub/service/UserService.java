package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.model.User;
import org.podhub.podhub.model.enums.UserRole;
import org.podhub.podhub.model.enums.UserStatus;
import org.podhub.podhub.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<User> findByStatus(UserStatus status, Pageable pageable) {
        return userRepository.findByStatus(status, pageable);
    }

    public Page<User> findByRole(UserRole role, Pageable pageable) {
        return userRepository.findByRole(role, pageable);
    }

    public Page<User> searchByName(String name, Pageable pageable) {
        return userRepository.findByNameContainingIgnoreCase(name, pageable);
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
}
