package org.podhub.podhub.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.dto.ProgressRequest;
import org.podhub.podhub.model.ListeningProgress;
import org.podhub.podhub.service.ListeningProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST controller for user listening progress management.
 * Manages episode listening progress for users with RESTful endpoints.
 * Base path: /api/users/{userId}/progress
 */
@RestController
@RequestMapping("/api/users/{userId}/progress")
@RequiredArgsConstructor
public class UserProgressController {

    private final ListeningProgressService listeningProgressService;

    /**
     * PUT /api/users/{userId}/progress/{episodeId}
     * Create or update (upsert) listening progress for a user on an episode
     * Uses PUT for idempotent upsert semantics
     *
     * @param userId The user ID (from path)
     * @param episodeId The episode ID (from path)
     * @param request Request body containing positionSeconds and completed flag
     * @return Created/updated progress
     */
    @PutMapping("/{episodeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ListeningProgress> upsertProgress(
            @PathVariable String userId,
            @PathVariable String episodeId,
            @Valid @RequestBody ProgressRequest request) {
        ListeningProgress saved = listeningProgressService.upsert(
                userId,
                episodeId,
                request.getPositionSeconds(),
                request.getCompleted()
        );
        return ResponseEntity.ok(saved);
    }

    /**
     * GET /api/users/{userId}/progress?cursor={timestamp}&limit={number}
     * List all listening progress for a user with cursor-based pagination
     *
     * @param userId The user ID
     * @param cursor Optional timestamp cursor for pagination
     * @param limit Page size (default: 20)
     * @return Paginated list of listening progress
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginatedResponse<ListeningProgress>> getUserProgress(
            @PathVariable String userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<ListeningProgress> response = listeningProgressService.findByUserId(userId, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/users/{userId}/progress/{episodeId}
     * Get listening progress for a specific user and episode
     *
     * @param userId The user ID
     * @param episodeId The episode ID
     * @return Listening progress or 404 if not found
     */
    @GetMapping("/{episodeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ListeningProgress> getEpisodeProgress(
            @PathVariable String userId,
            @PathVariable String episodeId) {
        return listeningProgressService.findByUserAndEpisode(userId, episodeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/users/{userId}/progress/{episodeId}
     * Delete (reset) listening progress for a user on an episode
     *
     * @param userId The user ID
     * @param episodeId The episode ID
     * @return 204 No Content on success, 404 if not found
     */
    @DeleteMapping("/{episodeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteProgress(
            @PathVariable String userId,
            @PathVariable String episodeId) {
        try {
            listeningProgressService.delete(userId, episodeId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
