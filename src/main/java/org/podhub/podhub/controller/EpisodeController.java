package org.podhub.podhub.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.model.Episode;
import org.podhub.podhub.service.EpisodeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/episodes")
@RequiredArgsConstructor
public class EpisodeController {

    private final EpisodeService episodeService;

    /**
     * POST /api/episodes
     * Crea un nuevo episodio asociado a un podcast.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('EPISODE_WRITE')")
    public ResponseEntity<Episode> createEpisode(@Valid @RequestBody Episode episode) {
        Episode created = episodeService.createEpisode(episode);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/episodes/{id}
     * Obtiene los detalles de un episodio por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Episode> getEpisodeById(@PathVariable String id) {
        return episodeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/episodes/{id}
     * Actualiza los datos de un episodio existente.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EPISODE_WRITE')")
    public ResponseEntity<Episode> updateEpisode(
            @PathVariable String id,
            @Valid @RequestBody Episode episode) {
        try {
            Episode updated = episodeService.updateEpisode(id, episode);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/episodes/{id}
     * Elimina un episodio por su ID.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EPISODE_WRITE')")
    public ResponseEntity<Void> deleteEpisode(@PathVariable String id) {
        try {
            episodeService.deleteEpisode(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/episodes?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista todos los episodios con paginación cursor-based
     *
     * Primera página: GET /api/episodes?limit=20
     * Siguiente página: GET /api/episodes?cursor={nextCursor}&limit=20
     */
    @GetMapping
    @PreAuthorize("hasAuthority('EPISODE_READ')")
    public ResponseEntity<PaginatedResponse<Episode>> getAllEpisodes(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<Episode> response = episodeService.findAll(cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/episodes/public?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista solo episodios públicos con paginación cursor-based
     */
    @GetMapping("/public")
    public ResponseEntity<PaginatedResponse<Episode>> getPublicEpisodes(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<Episode> response = episodeService.findPublicEpisodes(cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/episodes/podcast/{podcastId}?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista episodios de un podcast específico con paginación cursor-based
     */
    @GetMapping("/podcast/{podcastId}")
    public ResponseEntity<PaginatedResponse<Episode>> getEpisodesByPodcast(
            @PathVariable String podcastId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<Episode> response = episodeService.findByPodcastId(podcastId, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/episodes/search?title=tech&cursor=2024-01-15T10:30:00Z&limit=20
     * Busca episodios por título con paginación cursor-based
     */
    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<Episode>> searchEpisodes(
            @RequestParam String title,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<Episode> response = episodeService.searchByTitle(title, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }
}
