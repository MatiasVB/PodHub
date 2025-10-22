package org.podhub.podhub.repository;

import org.podhub.podhub.model.Podcast;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PodcastRepository extends MongoRepository<Podcast, String> {

    // ========== Búsquedas individuales ==========

    Optional<Podcast> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // ========== Paginación cursor-based: Todos los podcasts ==========

    @Query(value = "{}", sort = "{ 'createdAt': -1 }")
    List<Podcast> findFirstPodcasts(int limit);

    @Query(value = "{ 'createdAt': { $lt: ?0 } }", sort = "{ 'createdAt': -1 }")
    List<Podcast> findNextPodcasts(Instant cursor, int limit);

    // ========== Paginación cursor-based: Podcasts públicos ==========

    @Query(value = "{ 'isPublic': true }", sort = "{ 'createdAt': -1 }")
    List<Podcast> findFirstPublicPodcasts(int limit);

    @Query(value = "{ 'isPublic': true, 'createdAt': { $lt: ?0 } }", sort = "{ 'createdAt': -1 }")
    List<Podcast> findNextPublicPodcasts(Instant cursor, int limit);

    // ========== Paginación cursor-based: Por creador ==========

    @Query(value = "{ 'creatorId': ?0 }", sort = "{ 'createdAt': -1 }")
    List<Podcast> findFirstPodcastsByCreator(String creatorId, int limit);

    @Query(value = "{ 'creatorId': ?0, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<Podcast> findNextPodcastsByCreator(String creatorId, Instant cursor, int limit);

    // ========== Paginación cursor-based: Búsqueda por título ==========

    @Query(value = "{ 'title': { $regex: ?0, $options: 'i' } }", sort = "{ 'createdAt': -1 }")
    List<Podcast> findFirstPodcastsByTitle(String title, int limit);

    @Query(value = "{ 'title': { $regex: ?0, $options: 'i' }, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<Podcast> findNextPodcastsByTitle(String title, Instant cursor, int limit);
}
