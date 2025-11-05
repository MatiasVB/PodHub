package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.dto.PaginatedResponse;
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

    private static final int DEFAULT_MAX_LIMIT = 100;

    private final SubscriptionRepository subscriptionRepository;

    public Subscription subscribe(String userId, String podcastId) {
        ensureNotBlank(userId, "userId");
        ensureNotBlank(podcastId, "podcastId");

        log.debug("User {} subscribing to podcast {}", userId, podcastId);

        if (subscriptionRepository.existsByUserIdAndPodcastId(userId, podcastId)) {
            throw new IllegalArgumentException("La suscripción ya existe para este usuario y podcast");
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
        ensureNotBlank(userId, "userId");
        ensureNotBlank(podcastId, "podcastId");

        log.debug("User {} unsubscribing from podcast {}", userId, podcastId);
        subscriptionRepository.findByUserIdAndPodcastId(userId, podcastId)
                .ifPresent(subscriptionRepository::delete);
    }

    public Optional<Subscription> findById(String id) {
        return subscriptionRepository.findById(id);
    }

    /**
     * Obtiene las suscripciones de un usuario con paginación cursor-based
     *
     * @param userId Usuario que tiene las suscripciones
     * @param cursor Timestamp del último elemento (null para primera página)
     * @param limit  Número máximo de elementos a retornar
     * @return Respuesta paginada con cursor para siguiente página
     */
    public PaginatedResponse<Subscription> findByUserId(String userId, Instant cursor, int limit) {
        ensureNotBlank(userId, "userId");
        validateLimit(limit);

        log.debug("Finding subscriptions by user: {} with cursor: {} and limit: {}", userId, cursor, limit);

        List<Subscription> subscriptions;
        if (cursor == null) {
            subscriptions = subscriptionRepository.findFirstSubscriptionsByUser(userId, limit + 1);
        } else {
            subscriptions = subscriptionRepository.findNextSubscriptionsByUser(userId, cursor, limit + 1);
        }

        return buildPaginatedResponse(subscriptions, limit);
    }

    /**
     * Obtiene los suscriptores de un podcast con paginación cursor-based
     */
    public PaginatedResponse<Subscription> findByPodcastId(String podcastId, Instant cursor, int limit) {
        ensureNotBlank(podcastId, "podcastId");
        validateLimit(limit);

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
        ensureNotBlank(podcastId, "podcastId");
        return subscriptionRepository.countByPodcastId(podcastId);
    }

    /**
     * Construye la respuesta paginada a partir de una lista de suscripciones
     *
     * @param subscriptions Lista con limit+1 elementos
     * @param limit         Límite real solicitado
     * @return PaginatedResponse con nextCursor si hay más elementos
     */
    private PaginatedResponse<Subscription> buildPaginatedResponse(List<Subscription> subscriptions, int limit) {
        boolean hasMore = subscriptions.size() > limit;

        // Si hay más elementos, solo retornamos los primeros 'limit'
        List<Subscription> data;
        if (hasMore) {
            data = subscriptions.subList(0, limit);
        } else {
            data = subscriptions;
        }

        // Calcular el nextCursor (createdAt del último elemento)
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

    /*================= Helpers =================*/

    private void ensureNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " no puede estar vacío");
        }
    }

    private void validateLimit(int limit) {
        if (limit <= 0 || limit > DEFAULT_MAX_LIMIT) {
            throw new IllegalArgumentException("Limit must be between 1 and " + DEFAULT_MAX_LIMIT);
        }
    }
}
