package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.model.Subscription;
import org.podhub.podhub.repository.SubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public Subscription subscribe(String userId, String podcastId) {
        log.debug("User {} subscribing to podcast {}", userId, podcastId);
        if (subscriptionRepository.existsByUserIdAndPodcastId(userId, podcastId)) {
            throw new IllegalArgumentException("Subscription already exists");
        }
        Subscription sub = Subscription.builder()
                .userId(userId)
                .podcastId(podcastId)
                .createdAt(Instant.now())
                .build();
        Subscription saved = subscriptionRepository.save(sub);
        log.info("Subscription created {}", saved.getId());
        return saved;
    }

    public void unsubscribe(String userId, String podcastId) {
        log.debug("User {} unsubscribing from podcast {}", userId, podcastId);
        subscriptionRepository.findByUserIdAndPodcastId(userId, podcastId)
                .ifPresent(subscriptionRepository::delete);
    }

    public Optional<Subscription> findById(String id) {
        return subscriptionRepository.findById(id);
    }

    public Page<Subscription> findByUserId(String userId, Pageable pageable) {
        return subscriptionRepository.findByUserId(userId, pageable);
    }

    public Page<Subscription> findByPodcastId(String podcastId, Pageable pageable) {
        return subscriptionRepository.findByPodcastId(podcastId, pageable);
    }

    public long countByPodcastId(String podcastId) {
        return subscriptionRepository.countByPodcastId(podcastId);
    }
}
