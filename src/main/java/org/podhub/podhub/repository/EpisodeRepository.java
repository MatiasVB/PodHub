package org.podhub.podhub.repository;

import org.podhub.podhub.model.Episode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EpisodeRepository extends MongoRepository<Episode, String> {

    Page<Episode> findByPodcastId(String podcastId, Pageable pageable);

    Page<Episode> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Episode> findByIsPublicTrue(Pageable pageable);

    long countByPodcastId(String podcastId);

    // ========== Paginación cursor-based: Todos los episodios ==========

    @Query(value = "{}", sort = "{ 'createdAt': -1 }")
    List<Episode> findFirstEpisodes(int limit);

    @Query(value = "{ 'createdAt': { $lt: ?0 } }", sort = "{ 'createdAt': -1 }")
    List<Episode> findNextEpisodes(Instant cursor, int limit);

    // ========== Paginación cursor-based: Por podcast ==========

    @Query(value = "{ 'podcastId': ?0 }", sort = "{ 'createdAt': -1 }")
    List<Episode> findFirstEpisodesByPodcast(String podcastId, int limit);

    @Query(value = "{ 'podcastId': ?0, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<Episode> findNextEpisodesByPodcast(String podcastId, Instant cursor, int limit);

    // ========== Paginación cursor-based: Búsqueda por título ==========

    @Query(value = "{ 'title': { $regex: ?0, $options: 'i' } }", sort = "{ 'createdAt': -1 }")
    List<Episode> findFirstEpisodesByTitle(String title, int limit);

    @Query(value = "{ 'title': { $regex: ?0, $options: 'i' }, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<Episode> findNextEpisodesByTitle(String title, Instant cursor, int limit);

    // ========== Paginación cursor-based: Episodios públicos ==========

    @Query(value = "{ 'isPublic': true }", sort = "{ 'createdAt': -1 }")
    List<Episode> findFirstPublicEpisodes(int limit);

    @Query(value = "{ 'isPublic': true, 'createdAt': { $lt: ?0 } }", sort = "{ 'createdAt': -1 }")
    List<Episode> findNextPublicEpisodes(Instant cursor, int limit);
}
