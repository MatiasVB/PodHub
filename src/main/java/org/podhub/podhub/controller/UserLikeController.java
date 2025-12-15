package org.podhub.podhub.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.podhub.podhub.dto.LikeRequest;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.model.EpisodeLike;
import org.podhub.podhub.service.EpisodeLikeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST controller for user episode likes management.
 * Manages episode likes for users with RESTful endpoints.
 * Base path: /api/users/{userId}/likes
 */
@RestController
@RequestMapping("/api/users/{userId}/likes")
@RequiredArgsConstructor
public class UserLikeController {

    private final EpisodeLikeService episodeLikeService;

    /**
     * POST /api/users/{userId}/likes
     * Like an episode
     *
     * @param userId The user ID (from path)
     * @param request Request body containing episodeId
     * @return Created like
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EpisodeLike> like(
            @PathVariable String userId,
            @Valid @RequestBody LikeRequest request) {
        EpisodeLike created = episodeLikeService.like(userId, request.getEpisodeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/users/{userId}/likes?cursor={timestamp}&limit={number}
     * List all likes for a user with cursor-based pagination
     *
     * @param userId The user ID
     * @param cursor Optional timestamp cursor for pagination
     * @param limit Page size (default: 20)
     * @return Paginated list of likes
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginatedResponse<EpisodeLike>> getUserLikes(
            @PathVariable String userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<EpisodeLike> response = episodeLikeService.findByUserId(userId, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/users/{userId}/likes/{episodeId}
     * Unlike an episode
     *
     * @param userId The user ID
     * @param episodeId The episode ID to unlike
     * @return 204 No Content on success, 404 if like doesn't exist
     */
    @DeleteMapping("/{episodeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unlike(
            @PathVariable String userId,
            @PathVariable String episodeId) {
        try {
            episodeLikeService.unlike(userId, episodeId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * HEAD /api/users/{userId}/likes/{episodeId}
     * Check if a like exists (for existence check)
     *
     * @param userId The user ID
     * @param episodeId The episode ID
     * @return 200 OK if like exists, 404 Not Found if it doesn't
     */
    @RequestMapping(value = "/{episodeId}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkLikeExists(
            @PathVariable String userId,
            @PathVariable String episodeId) {
        boolean exists = episodeLikeService.exists(userId, episodeId);
        return exists ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
