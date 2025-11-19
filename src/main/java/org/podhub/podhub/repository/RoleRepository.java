package org.podhub.podhub.repository;

import org.podhub.podhub.model.Role;
import org.podhub.podhub.model.enums.UserRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends MongoRepository<Role, String> {

    // ========== Búsquedas individuales ==========
    Optional<Role> findByName(UserRole name); // p.ej. ROLE_ADMIN
    boolean existsByName(UserRole name);

    // ========== Paginación cursor-based: Todos ==========
    @Query(value = "{}", sort = "{ 'createdAt': -1 }")
    List<Role> findFirstRoles(int limit);

    @Query(value = "{ 'createdAt': { $lt: ?0 } }", sort = "{ 'createdAt': -1 }")
    List<Role> findNextRoles(Instant cursor, int limit);

    // ========== Paginación cursor-based: Búsqueda por nombre ==========
    @Query(value = "{ 'name': { $regex: ?0, $options: 'i' } }", sort = "{ 'createdAt': -1 }")
    List<Role> findFirstRolesByName(String name, int limit);

    @Query(value = "{ 'name': { $regex: ?0, $options: 'i' }, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<Role> findNextRolesByName(String name, Instant cursor, int limit);
}
