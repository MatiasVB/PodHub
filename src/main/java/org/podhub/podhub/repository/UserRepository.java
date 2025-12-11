package org.podhub.podhub.repository;

import org.podhub.podhub.model.User;
import org.podhub.podhub.model.enums.UserRole;
import org.podhub.podhub.model.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    Page<User> findByRole(UserRole role, Pageable pageable);

    // ========== Paginación cursor-based: Todos los usuarios ==========

    @Query(value = "{}", sort = "{ 'createdAt': -1 }")
    List<User> findFirstUsers(int limit);

    @Query(value = "{ 'createdAt': { $lt: ?0 } }", sort = "{ 'createdAt': -1 }")
    List<User> findNextUsers(Instant cursor, int limit);

    // ========== Paginación cursor-based: Por estado ==========

    @Query(value = "{ 'status': ?0 }", sort = "{ 'createdAt': -1 }")
    List<User> findFirstUsersByStatus(UserStatus status, int limit);

    @Query(value = "{ 'status': ?0, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<User> findNextUsersByStatus(UserStatus status, Instant cursor, int limit);

    // ========== Paginación cursor-based: Por rol ==========

    @Query(value = "{ 'role': ?0 }", sort = "{ 'createdAt': -1 }")
    List<User> findFirstUsersByRole(UserRole role, int limit);

    @Query(value = "{ 'role': ?0, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<User> findNextUsersByRole(UserRole role, Instant cursor, int limit);

    // ========== Paginación cursor-based: Búsqueda por nombre ==========
    // Busca en username O displayName usando $or

    @Query(value = "{ $or: [ { 'username': { $regex: ?0, $options: 'i' } }, { 'displayName': { $regex: ?0, $options: 'i' } } ] }", sort = "{ 'createdAt': -1 }")
    List<User> findFirstUsersByName(String name, int limit);

    @Query(value = "{ $or: [ { 'username': { $regex: ?0, $options: 'i' } }, { 'displayName': { $regex: ?0, $options: 'i' } } ], 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<User> findNextUsersByName(String name, Instant cursor, int limit);
}
