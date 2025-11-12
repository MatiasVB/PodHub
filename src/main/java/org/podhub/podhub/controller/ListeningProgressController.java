package org.podhub.podhub.controller;

import lombok.RequiredArgsConstructor;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.model.ListeningProgress;
import org.podhub.podhub.service.ListeningProgressService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ListeningProgressController {

    private final ListeningProgressService listeningProgressService;

    /**
     * POST /api/progress?userId={userId}&episodeId={episodeId}&positionSeconds={n}&completed={true|false}
     * Crea o actualiza (upsert) el progreso de escucha de un usuario en un episodio
     */
    @PostMapping
    public ResponseEntity<ListeningProgress> upsertProgress(
            @RequestParam String userId,
            @RequestParam String episodeId,
            @RequestParam int positionSeconds,
            @RequestParam(defaultValue = "false") boolean completed) {
        ListeningProgress saved = listeningProgressService.upsert(userId, episodeId, positionSeconds, completed);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * GET /api/progress/{id}
     * Obtiene un progreso por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ListeningProgress> getProgressById(@PathVariable String id) {
        return listeningProgressService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/progress/one?userId={userId}&episodeId={episodeId}
     * Obtiene el progreso de un usuario en un episodio concreto
     */
    @GetMapping("/one")
    public ResponseEntity<ListeningProgress> getOne(
            @RequestParam String userId,
            @RequestParam String episodeId) {
        return listeningProgressService.findByUserAndEpisode(userId, episodeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/progress?userId={userId}&episodeId={episodeId}
     * Elimina (resetea) el progreso de un usuario en un episodio
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteProgress(
            @RequestParam String userId,
            @RequestParam String episodeId) {
        try {
            listeningProgressService.delete(userId, episodeId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/progress/user/{userId}?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista progresos de un usuario con paginación cursor-based
     *
     * Primera página: GET /api/progress/user/{userId}?limit=20
     * Siguiente página: GET /api/progress/user/{userId}?cursor={nextCursor}&limit=20
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<PaginatedResponse<ListeningProgress>> getProgressByUser(
            @PathVariable String userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<ListeningProgress> response =
                listeningProgressService.findByUserId(userId, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/progress/episode/{episodeId}?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista progresos de un episodio con paginación cursor-based
     */
    @GetMapping("/episode/{episodeId}")
    public ResponseEntity<PaginatedResponse<ListeningProgress>> getProgressByEpisode(
            @PathVariable String episodeId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<ListeningProgress> response =
                listeningProgressService.findByEpisodeId(episodeId, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }
}
