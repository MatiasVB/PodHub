package org.podhub.podhub.repository;

import org.podhub.podhub.model.ListeningProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ListeningProgressRepository extends MongoRepository<ListeningProgress, String> {

    Optional<ListeningProgress> findByUserIdAndEpisodeId(String userId, String episodeId);

    Page<ListeningProgress> findByUserId(String userId, Pageable pageable);

    Page<ListeningProgress> findByEpisodeId(String episodeId, Pageable pageable);

    long countByEpisodeId(String episodeId);

    // ========== Paginación cursor-based: Por usuario ==========

    @Query(value = "{ 'userId': ?0 }", sort = "{ 'createdAt': -1 }")
    List<ListeningProgress> findFirstProgressByUser(String userId, int limit);

    @Query(value = "{ 'userId': ?0, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<ListeningProgress> findNextProgressByUser(String userId, Instant cursor, int limit);

    // ========== Paginación cursor-based: Por episodio ==========

    @Query(value = "{ 'episodeId': ?0 }", sort = "{ 'createdAt': -1 }")
    List<ListeningProgress> findFirstProgressByEpisode(String episodeId, int limit);

    @Query(value = "{ 'episodeId': ?0, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<ListeningProgress> findNextProgressByEpisode(String episodeId, Instant cursor, int limit);
}
