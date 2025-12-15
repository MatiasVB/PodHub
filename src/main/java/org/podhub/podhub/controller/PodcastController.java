package org.podhub.podhub.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.podhub.podhub.dto.CountResponse;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.model.Podcast;
import org.podhub.podhub.model.Subscription;
import org.podhub.podhub.service.PodcastService;
import org.podhub.podhub.service.SubscriptionService;
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
    private final SubscriptionService subscriptionService;

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
     * GET /api/podcasts/{idOrSlug}
     * Obtiene un podcast por ID o slug (URLs amigables)
     * Intenta primero como ID, si no existe intenta como slug
     */
    @GetMapping("/{idOrSlug}")
    public ResponseEntity<Podcast> getPodcast(@PathVariable String idOrSlug) {
        return podcastService.findByIdOrSlug(idOrSlug)
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
     * GET /api/podcasts?cursor={timestamp}&limit={number}&isPublic={boolean}&creatorId={id}&title={query}
     * Lista podcasts con filtros opcionales y paginación cursor-based
     *
     * Ejemplos:
     * - GET /api/podcasts?limit=20                        (todos los podcasts)
     * - GET /api/podcasts?isPublic=true                   (solo públicos)
     * - GET /api/podcasts?creatorId={id}                  (por creador)
     * - GET /api/podcasts?title=tech                      (búsqueda por título)
     * - GET /api/podcasts?cursor={timestamp}&limit=20     (siguiente página)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PODCAST_READ')")
    public ResponseEntity<PaginatedResponse<Podcast>> getAllPodcasts(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(required = false) String creatorId,
            @RequestParam(required = false) String title) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<Podcast> response = podcastService.findAll(cursorInstant, limit, isPublic, creatorId, title);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/podcasts/{podcastId}/subscribers?cursor={timestamp}&limit={number}&count={boolean}
     * List subscribers of a podcast or get subscriber count
     *
     * @param podcastId The podcast ID
     * @param cursor Optional timestamp cursor for pagination
     * @param limit Page size (default: 20)
     * @param count If true, returns only count; if false/null, returns paginated list
     * @return Paginated list of subscriptions or count response
     */
    @GetMapping("/{podcastId}/subscribers")
    public ResponseEntity<?> getPodcastSubscribers(
            @PathVariable String podcastId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Boolean count) {
        if (Boolean.TRUE.equals(count)) {
            long subscriberCount = subscriptionService.countByPodcastId(podcastId);
            return ResponseEntity.ok(CountResponse.builder().count(subscriberCount).build());
        } else {
            Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
            PaginatedResponse<Subscription> response = subscriptionService.findByPodcastId(podcastId, cursorInstant, limit);
            return ResponseEntity.ok(response);
        }
    }
}
