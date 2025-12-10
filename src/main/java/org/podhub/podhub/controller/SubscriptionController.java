package org.podhub.podhub.controller;

import lombok.RequiredArgsConstructor;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.model.Subscription;
import org.podhub.podhub.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * POST /api/subscriptions/subscribe?userId={userId}&podcastId={podcastId}
     * Crea una suscripción (userId se suscribe a podcastId)
     */
    @PostMapping("/subscribe")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Subscription> subscribe(
            @RequestParam String userId,
            @RequestParam String podcastId) {
        Subscription created = subscriptionService.subscribe(userId, podcastId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * DELETE /api/subscriptions/unsubscribe?userId={userId}&podcastId={podcastId}
     * Elimina una suscripción (dejar de seguir)
     */
    @DeleteMapping("/unsubscribe")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unsubscribe(
            @RequestParam String userId,
            @RequestParam String podcastId) {
        subscriptionService.unsubscribe(userId, podcastId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/subscriptions/{id}
     * Obtiene una suscripción por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Subscription> getSubscriptionById(@PathVariable String id) {
        return subscriptionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/subscriptions/user/{userId}?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista suscripciones de un usuario con paginación cursor-based
     *
     * Primera página: GET /api/subscriptions/user/{userId}?limit=20
     * Siguiente página: GET /api/subscriptions/user/{userId}?cursor={nextCursor}&limit=20
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginatedResponse<Subscription>> getSubscriptionsByUser(
            @PathVariable String userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<Subscription> response = subscriptionService.findByUserId(userId, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/subscriptions/podcast/{podcastId}?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista suscriptores de un podcast con paginación cursor-based
     */
    @GetMapping("/podcast/{podcastId}")
    public ResponseEntity<PaginatedResponse<Subscription>> getSubscriptionsByPodcast(
            @PathVariable String podcastId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<Subscription> response = subscriptionService.findByPodcastId(podcastId, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/subscriptions/podcast/{podcastId}/count
     * Devuelve el número total de suscriptores de un podcast
     */
    @GetMapping("/podcast/{podcastId}/count")
    public ResponseEntity<Long> countByPodcast(@PathVariable String podcastId) {
        long count = subscriptionService.countByPodcastId(podcastId);
        return ResponseEntity.ok(count);
    }
}
