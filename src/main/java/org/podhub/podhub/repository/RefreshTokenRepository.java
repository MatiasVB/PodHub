package org.podhub.podhub.repository;

import org.podhub.podhub.model.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    // ========== Búsquedas individuales ==========
    Optional<RefreshToken> findByToken(String token);
    boolean existsByToken(String token);
    // Revocar en bloque (cargar y luego marcar revoked=true en servicio)
    List<RefreshToken> findAllByUserIdAndRevokedFalse(String userId);

    // ========== Paginación cursor-based: Por usuario ==========
    @Query(value = "{ 'userId': ?0 }", sort = "{ 'createdAt': -1 }")
    List<RefreshToken> findFirstTokensByUser(String userId, int limit);

    @Query(value = "{ 'userId': ?0, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<RefreshToken> findNextTokensByUser(String userId, Instant cursor, int limit);

    // ========== Paginación cursor-based: Por usuario (solo activos: !revoked) ==========
    @Query(value = "{ 'userId': ?0, 'revoked': false }", sort = "{ 'createdAt': -1 }")
    List<RefreshToken> findFirstActiveTokensByUser(String userId, int limit);

    @Query(value = "{ 'userId': ?0, 'revoked': false, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<RefreshToken> findNextActiveTokensByUser(String userId, Instant cursor, int limit);

    // ========== Limpieza/consulta de expirados ==========
    List<RefreshToken> findByExpiresAtBefore(Instant instant);

    long deleteByExpiresAtBefore(Instant instant);
}

