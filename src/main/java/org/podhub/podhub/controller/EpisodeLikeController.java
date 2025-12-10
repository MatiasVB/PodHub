package org.podhub.podhub.controller;

import lombok.RequiredArgsConstructor;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.model.EpisodeLike;
import org.podhub.podhub.service.EpisodeLikeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class EpisodeLikeController {

    private final EpisodeLikeService episodeLikeService;

    /**
     * POST /api/likes/like?userId={userId}&episodeId={episodeId}
     * Crea un like de un usuario sobre un episodio
     */
    @PostMapping("/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EpisodeLike> like(
            @RequestParam String userId,
            @RequestParam String episodeId) {
        EpisodeLike created = episodeLikeService.like(userId, episodeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * DELETE /api/likes/unlike?userId={userId}&episodeId={episodeId}
     * Elimina el like de un usuario sobre un episodio
     */
    @DeleteMapping("/unlike")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unlike(
            @RequestParam String userId,
            @RequestParam String episodeId) {
        try {
            episodeLikeService.unlike(userId, episodeId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            // Si no existía el like (o no coincide), devolvemos 404 como en el resto de controladores
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/likes/{id}
     * Obtiene un like por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<EpisodeLike> getLikeById(@PathVariable String id) {
        return episodeLikeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/likes/user/{userId}?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista likes de un usuario con paginación cursor-based
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginatedResponse<EpisodeLike>> getLikesByUser(
            @PathVariable String userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<EpisodeLike> response =
                episodeLikeService.findByUserId(userId, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/likes/episode/{episodeId}?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista likes de un episodio con paginación cursor-based
     */
    @GetMapping("/episode/{episodeId}")
    public ResponseEntity<PaginatedResponse<EpisodeLike>> getLikesByEpisode(
            @PathVariable String episodeId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<EpisodeLike> response =
                episodeLikeService.findByEpisodeId(episodeId, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/likes/exists?userId={userId}&episodeId={episodeId}
     * Comprueba si el usuario ya ha dado like a un episodio
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> exists(
            @RequestParam String userId,
            @RequestParam String episodeId) {
        boolean exists = episodeLikeService.exists(userId, episodeId);
        return ResponseEntity.ok(exists);
    }

    /**
     * GET /api/likes/episode/{episodeId}/count
     * Devuelve el número total de likes de un episodio
     */
    @GetMapping("/episode/{episodeId}/count")
    public ResponseEntity<Long> countByEpisode(@PathVariable String episodeId) {
        long count = episodeLikeService.countByEpisodeId(episodeId);
        return ResponseEntity.ok(count);
    }
}
