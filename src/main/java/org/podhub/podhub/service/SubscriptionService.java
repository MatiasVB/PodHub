package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.exception.ConflictException;
import org.podhub.podhub.exception.ResourceNotFoundException;
import org.podhub.podhub.model.Subscription;
import org.podhub.podhub.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public Subscription subscribe(String userId, String podcastId) {
        log.debug("User {} subscribing to podcast {}", userId, podcastId);
        if (subscriptionRepository.existsByUserIdAndPodcastId(userId, podcastId)) {
            throw new ConflictException("Subscription already exists");
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
        Optional<Subscription> existing = subscriptionRepository.findByUserIdAndPodcastId(userId, podcastId);
        Subscription toDelete = existing.orElseThrow(() ->
                new ResourceNotFoundException("Subscription not found for user and podcast"));
        subscriptionRepository.delete(toDelete);
    }

    public Optional<Subscription> findById(String id) {
        return subscriptionRepository.findById(id);
    }

    public PaginatedResponse<Subscription> findByUserId(String userId, Instant cursor, int limit) {
        log.debug("Finding subscriptions by user: {} with cursor: {} and limit: {}", userId, cursor, limit);

        List<Subscription> subscriptions;
        if (cursor == null) {
            subscriptions = subscriptionRepository.findFirstSubscriptionsByUser(userId, limit + 1);
        } else {
            subscriptions = subscriptionRepository.findNextSubscriptionsByUser(userId, cursor, limit + 1);
        }

        return buildPaginatedResponse(subscriptions, limit);
    }

    public PaginatedResponse<Subscription> findByPodcastId(String podcastId, Instant cursor, int limit) {
        log.debug("Finding subscriptions by podcast: {} with cursor: {} and limit: {}", podcastId, cursor, limit);

        List<Subscription> subscriptions;
        if (cursor == null) {
            subscriptions = subscriptionRepository.findFirstSubscriptionsByPodcast(podcastId, limit + 1);
        } else {
            subscriptions = subscriptionRepository.findNextSubscriptionsByPodcast(podcastId, cursor, limit + 1);
        }

        return buildPaginatedResponse(subscriptions, limit);
    }

    public long countByPodcastId(String podcastId) {
        return subscriptionRepository.countByPodcastId(podcastId);
    }

    private PaginatedResponse<Subscription> buildPaginatedResponse(List<Subscription> subscriptions, int limit) {
        boolean hasMore = subscriptions.size() > limit;

        List<Subscription> data;
        if (hasMore) {
            data = subscriptions.subList(0, limit);
        } else {
            data = subscriptions;
        }

        String nextCursor = null;
        if (hasMore) {
            if (!data.isEmpty()) {
                nextCursor = data.get(data.size() - 1).getCreatedAt().toString();
            }
        }

        return PaginatedResponse.<Subscription>builder()
                .data(data)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .count(data.size())
                .build();
    }
}
