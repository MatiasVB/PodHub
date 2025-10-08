package org.podhub.podhub.repository;

import org.podhub.podhub.model.User;
import org.podhub.podhub.model.enums.UserRole;
import org.podhub.podhub.model.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    Page<User> findByRole(UserRole role, Pageable pageable);

    // Si tienes un campo 'name' o 'displayName':
    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
