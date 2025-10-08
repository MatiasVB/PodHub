package org.podhub.podhub.repository;

import org.podhub.podhub.model.Subscription;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface SubscriptionRepository extends MongoRepository<Subscription, String> {

    boolean existsByUserIdAndPodcastId(String userId, String podcastId);

    Optional<Subscription> findByUserIdAndPodcastId(String userId, String podcastId);

    Page<Subscription> findByUserId(String userId, Pageable pageable);

    Page<Subscription> findByPodcastId(String podcastId, Pageable pageable);

    long countByPodcastId(String podcastId);
}
