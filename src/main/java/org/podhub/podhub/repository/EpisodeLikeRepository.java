package org.podhub.podhub.repository;

import org.podhub.podhub.model.EpisodeLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EpisodeLikeRepository extends MongoRepository<EpisodeLike, String> {

    boolean existsByUserIdAndEpisodeId(String userId, String episodeId);

    Optional<EpisodeLike> findByUserIdAndEpisodeId(String userId, String episodeId);

    Page<EpisodeLike> findByEpisodeId(String episodeId, Pageable pageable);

    Page<EpisodeLike> findByUserId(String userId, Pageable pageable);

    long countByEpisodeId(String episodeId);

    // ========== Paginación cursor-based: Por episodio ==========

    @Query(value = "{ 'episodeId': ?0 }", sort = "{ 'createdAt': -1 }")
    List<EpisodeLike> findFirstLikesByEpisode(String episodeId, int limit);

    @Query(value = "{ 'episodeId': ?0, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<EpisodeLike> findNextLikesByEpisode(String episodeId, Instant cursor, int limit);

    // ========== Paginación cursor-based: Por usuario ==========

    @Query(value = "{ 'userId': ?0 }", sort = "{ 'createdAt': -1 }")
    List<EpisodeLike> findFirstLikesByUser(String userId, int limit);

    @Query(value = "{ 'userId': ?0, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<EpisodeLike> findNextLikesByUser(String userId, Instant cursor, int limit);
}
