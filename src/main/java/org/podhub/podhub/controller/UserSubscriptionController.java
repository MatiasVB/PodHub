package org.podhub.podhub.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.dto.SubscriptionRequest;
import org.podhub.podhub.model.Subscription;
import org.podhub.podhub.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST controller for user subscription management.
 * Manages podcast subscriptions for users with RESTful endpoints.
 * Base path: /api/users/{userId}/subscriptions
 */
@RestController
@RequestMapping("/api/users/{userId}/subscriptions")
@RequiredArgsConstructor
public class UserSubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * POST /api/users/{userId}/subscriptions
     * Subscribe a user to a podcast
     *
     * @param userId The user ID (from path)
     * @param request Request body containing podcastId
     * @return Created subscription
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Subscription> subscribe(
            @PathVariable String userId,
            @Valid @RequestBody SubscriptionRequest request) {
        Subscription created = subscriptionService.subscribe(userId, request.getPodcastId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/users/{userId}/subscriptions?cursor={timestamp}&limit={number}
     * List all subscriptions for a user with cursor-based pagination
     *
     * @param userId The user ID
     * @param cursor Optional timestamp cursor for pagination
     * @param limit Page size (default: 20)
     * @return Paginated list of subscriptions
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginatedResponse<Subscription>> getUserSubscriptions(
            @PathVariable String userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<Subscription> response = subscriptionService.findByUserId(userId, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/users/{userId}/subscriptions/{podcastId}
     * Unsubscribe a user from a podcast
     *
     * @param userId The user ID
     * @param podcastId The podcast ID to unsubscribe from
     * @return 204 No Content on success
     */
    @DeleteMapping("/{podcastId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unsubscribe(
            @PathVariable String userId,
            @PathVariable String podcastId) {
        subscriptionService.unsubscribe(userId, podcastId);
        return ResponseEntity.noContent().build();
    }
}
