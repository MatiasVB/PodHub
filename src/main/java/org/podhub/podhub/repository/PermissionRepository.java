package org.podhub.podhub.repository;

import org.podhub.podhub.model.Permission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends MongoRepository<Permission, String> {

    // ========== Búsquedas individuales ==========
    Optional<Permission> findByName(String name);
    boolean existsByName(String name);
    List<Permission> findByNameIn(Collection<String> names);

    // ========== Paginación cursor-based: Todas ==========
    @Query(value = "{}", sort = "{ 'createdAt': -1 }")
    List<Permission> findFirstPermissions(int limit);

    @Query(value = "{ 'createdAt': { $lt: ?0 } }", sort = "{ 'createdAt': -1 }")
    List<Permission> findNextPermissions(Instant cursor, int limit);

    // ========== Paginación cursor-based: Búsqueda por nombre (regex, case-insensitive) ==========
    @Query(value = "{ 'name': { $regex: ?0, $options: 'i' } }", sort = "{ 'createdAt': -1 }")
    List<Permission> findFirstPermissionsByName(String name, int limit);

    @Query(value = "{ 'name': { $regex: ?0, $options: 'i' }, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<Permission> findNextPermissionsByName(String name, Instant cursor, int limit);
}
