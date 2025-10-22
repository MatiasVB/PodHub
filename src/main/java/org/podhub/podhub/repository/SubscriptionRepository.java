package org.podhub.podhub.repository;

import org.podhub.podhub.model.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends MongoRepository<Subscription, String> {

    boolean existsByUserIdAndPodcastId(String userId, String podcastId);

    Optional<Subscription> findByUserIdAndPodcastId(String userId, String podcastId);

    Page<Subscription> findByUserId(String userId, Pageable pageable);

    Page<Subscription> findByPodcastId(String podcastId, Pageable pageable);

    long countByPodcastId(String podcastId);

    // ========== Paginación cursor-based: Por usuario ==========

    @Query(value = "{ 'userId': ?0 }", sort = "{ 'createdAt': -1 }")
    List<Subscription> findFirstSubscriptionsByUser(String userId, int limit);

    @Query(value = "{ 'userId': ?0, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<Subscription> findNextSubscriptionsByUser(String userId, Instant cursor, int limit);

    // ========== Paginación cursor-based: Por podcast ==========

    @Query(value = "{ 'podcastId': ?0 }", sort = "{ 'createdAt': -1 }")
    List<Subscription> findFirstSubscriptionsByPodcast(String podcastId, int limit);

    @Query(value = "{ 'podcastId': ?0, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<Subscription> findNextSubscriptionsByPodcast(String podcastId, Instant cursor, int limit);
}
