package org.podhub.podhub.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.podhub.podhub.dto.CountResponse;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.exception.ResourceNotFoundException;
import org.podhub.podhub.model.Episode;
import org.podhub.podhub.model.EpisodeLike;
import org.podhub.podhub.model.User;
import org.podhub.podhub.repository.UserRepository;
import org.podhub.podhub.service.EpisodeService;
import org.podhub.podhub.service.EpisodeLikeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/episodes")
@RequiredArgsConstructor
public class EpisodeController {

    private final EpisodeService episodeService;
    private final EpisodeLikeService episodeLikeService;
    private final UserRepository userRepository;

    /**
     * POST /api/episodes
     * Crea un nuevo episodio asociado a un podcast.
     * Solo el creador del podcast puede crear episodios en él.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('EPISODE_WRITE')")
    public ResponseEntity<Episode> createEpisode(
            @Valid @RequestBody Episode episode,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String userId = user.getId();
        Episode created = episodeService.createEpisode(episode, userId);
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
     * Solo el creador del podcast puede actualizar sus episodios.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EPISODE_WRITE')")
    public ResponseEntity<Episode> updateEpisode(
            @PathVariable String id,
            @Valid @RequestBody Episode episode,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String userId = user.getId();
        Episode updated = episodeService.updateEpisode(id, episode, userId);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/episodes/{id}
     * Elimina un episodio por su ID.
     * Solo el creador del podcast puede eliminar sus episodios.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EPISODE_WRITE')")
    public ResponseEntity<Void> deleteEpisode(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String userId = user.getId();
        episodeService.deleteEpisode(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/episodes?cursor={timestamp}&limit={number}&isPublic={boolean}&podcastId={id}&title={query}
     * Lista episodios con filtros opcionales y paginación cursor-based
     *
     * Ejemplos:
     * - GET /api/episodes?limit=20                        (todos los episodios)
     * - GET /api/episodes?isPublic=true                   (solo públicos)
     * - GET /api/episodes?podcastId={id}                  (por podcast)
     * - GET /api/episodes?title=tech                      (búsqueda por título)
     * - GET /api/episodes?cursor={timestamp}&limit=20     (siguiente página)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('EPISODE_READ')")
    public ResponseEntity<PaginatedResponse<Episode>> getAllEpisodes(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(required = false) String podcastId,
            @RequestParam(required = false) String title) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<Episode> response = episodeService.findAll(cursorInstant, limit, isPublic, podcastId, title);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/episodes/{episodeId}/likes?cursor={timestamp}&limit={number}&count={boolean}
     * List likes on an episode or get like count
     *
     * @param episodeId The episode ID
     * @param cursor Optional timestamp cursor for pagination
     * @param limit Page size (default: 20)
     * @param count If true, returns only count; if false/null, returns paginated list
     * @return Paginated list of likes or count response
     */
    @GetMapping("/{episodeId}/likes")
    public ResponseEntity<?> getEpisodeLikes(
            @PathVariable String episodeId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Boolean count) {
        if (Boolean.TRUE.equals(count)) {
            long likeCount = episodeLikeService.countByEpisodeId(episodeId);
            return ResponseEntity.ok(CountResponse.builder().count(likeCount).build());
        } else {
            Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
            PaginatedResponse<EpisodeLike> response = episodeLikeService.findByEpisodeId(episodeId, cursorInstant, limit);
            return ResponseEntity.ok(response);
        }
    }
}
