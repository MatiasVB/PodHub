package org.podhub.podhub.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.model.Podcast;
import org.podhub.podhub.service.PodcastService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/podcasts")
@RequiredArgsConstructor
public class PodcastController {

    private final PodcastService podcastService;

    /**
     * POST /api/podcasts
     * Crea un nuevo podcast
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PODCAST_WRITE')")
    public ResponseEntity<Podcast> createPodcast(@Valid @RequestBody Podcast podcast) {
        Podcast created = podcastService.createPodcast(podcast);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/podcasts/{id}
     * Obtiene un podcast por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Podcast> getPodcastById(@PathVariable String id) {
        return podcastService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/podcasts/slug/{slug}
     * Obtiene un podcast por slug (URLs amigables)
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<Podcast> getPodcastBySlug(@PathVariable String slug) {
        return podcastService.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/podcasts/{id}
     * Actualiza un podcast existente
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PODCAST_WRITE')")
    public ResponseEntity<Podcast> updatePodcast(
            @PathVariable String id,
            @Valid @RequestBody Podcast podcast) {
        try {
            Podcast updated = podcastService.updatePodcast(id, podcast);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/podcasts/{id}
     * Elimina un podcast por ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PODCAST_WRITE')")
    public ResponseEntity<Void> deletePodcast(@PathVariable String id) {
        try {
            podcastService.deletePodcast(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/podcasts?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista todos los podcasts con paginación cursor-based
     *
     * Primera página: GET /api/podcasts?limit=20
     * Siguiente página: GET /api/podcasts?cursor={nextCursor}&limit=20
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PODCAST_READ')")
    public ResponseEntity<PaginatedResponse<Podcast>> getAllPodcasts(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<Podcast> response = podcastService.findAll(cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/podcasts/public?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista solo podcasts públicos con paginación cursor-based
     */
    @GetMapping("/public")
    public ResponseEntity<PaginatedResponse<Podcast>> getPublicPodcasts(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<Podcast> response = podcastService.findPublicPodcasts(cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/podcasts/creator/{creatorId}?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista podcasts de un creador específico con paginación cursor-based
     */
    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<PaginatedResponse<Podcast>> getPodcastsByCreator(
            @PathVariable String creatorId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<Podcast> response = podcastService.findByCreatorId(creatorId, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/podcasts/search?title=tech&cursor=2024-01-15T10:30:00Z&limit=20
     * Busca podcasts por título con paginación cursor-based
     */
    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<Podcast>> searchPodcasts(
            @RequestParam String title,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<Podcast> response = podcastService.searchByTitle(title, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }
}
