package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.dto.EpisodePatchRequest;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.exception.ForbiddenException;
import org.podhub.podhub.exception.ResourceNotFoundException;
import org.podhub.podhub.model.Episode;
import org.podhub.podhub.model.Podcast;
import org.podhub.podhub.repository.EpisodeRepository;
import org.podhub.podhub.repository.PodcastRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeService {

    private final EpisodeRepository episodeRepository;
    private final PodcastRepository podcastRepository;

    /**
     * Validates that the given user owns the podcast to which the episode belongs
     *
     * @param podcastId ID of the podcast to check
     * @param userId ID of the user attempting the operation
     * @throws ResourceNotFoundException if podcast doesn't exist
     * @throws ForbiddenException if user doesn't own the podcast
     */
    private void validatePodcastOwnership(String podcastId, String userId) {
        Podcast podcast = podcastRepository.findById(podcastId)
                .orElseThrow(() -> new ResourceNotFoundException("Podcast not found with id: " + podcastId));

        if (!podcast.getCreatorId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to modify episodes in this podcast");
        }
    }

    /**
     * Creates a new episode
     * Validates that the user owns the podcast
     *
     * @param episode Episode to create
     * @param userId ID of the user creating the episode
     * @return Created episode
     * @throws ForbiddenException if user doesn't own the podcast
     */
    public Episode createEpisode(Episode episode, String userId) {
        log.debug("Creating episode '{}' in podcast {} by user {}", episode.getTitle(), episode.getPodcastId(), userId);

        // Verify that user owns the podcast
        validatePodcastOwnership(episode.getPodcastId(), userId);

        Instant now = Instant.now();
        episode.setCreatedAt(now);
        episode.setUpdatedAt(now);
        if (episode.getIsPublic() == null) {
            episode.setIsPublic(false);
        }
        Episode saved = episodeRepository.save(episode);
        log.info("Episode created {} by user {}", saved.getId(), userId);
        return saved;
    }

    public Optional<Episode> findById(String id) {
        return episodeRepository.findById(id);
    }

    /**
     * Obtiene todos los episodios con paginación cursor-based y filtros opcionales
     *
     * @param cursor Timestamp del último elemento (null para primera página)
     * @param limit Número máximo de elementos a retornar
     * @param isPublic Filtro opcional por visibilidad pública
     * @param podcastId Filtro opcional por podcast
     * @param title Filtro opcional por búsqueda en título
     * @return Respuesta paginada con cursor para siguiente página
     */
    public PaginatedResponse<Episode> findAll(Instant cursor, int limit, Boolean isPublic, String podcastId, String title) {
        log.debug("Finding episodes with cursor: {}, limit: {}, isPublic: {}, podcastId: {}, title: {}",
                  cursor, limit, isPublic, podcastId, title);

        List<Episode> episodes;

        // Determinar qué método del repository usar según los filtros
        if (title != null && !title.trim().isEmpty()) {
            // Búsqueda por título tiene prioridad
            if (cursor == null) {
                episodes = episodeRepository.findFirstEpisodesByTitle(title, limit + 1);
            } else {
                episodes = episodeRepository.findNextEpisodesByTitle(title, cursor, limit + 1);
            }
        } else if (podcastId != null && !podcastId.trim().isEmpty()) {
            // Filtro por podcast
            if (cursor == null) {
                episodes = episodeRepository.findFirstEpisodesByPodcast(podcastId, limit + 1);
            } else {
                episodes = episodeRepository.findNextEpisodesByPodcast(podcastId, cursor, limit + 1);
            }
        } else if (Boolean.TRUE.equals(isPublic)) {
            // Filtro por públicos
            if (cursor == null) {
                episodes = episodeRepository.findFirstPublicEpisodes(limit + 1);
            } else {
                episodes = episodeRepository.findNextPublicEpisodes(cursor, limit + 1);
            }
        } else {
            // Sin filtros, todos los episodios
            if (cursor == null) {
                episodes = episodeRepository.findFirstEpisodes(limit + 1);
            } else {
                episodes = episodeRepository.findNextEpisodes(cursor, limit + 1);
            }
        }

        return buildPaginatedResponse(episodes, limit);
    }

    /**
     * @deprecated Use findAll(cursor, limit, isPublic, podcastId, title) instead
     */
    @Deprecated
    public PaginatedResponse<Episode> findByPodcastId(String podcastId, Instant cursor, int limit) {
        return findAll(cursor, limit, null, podcastId, null);
    }

    /**
     * @deprecated Use findAll(cursor, limit, isPublic, podcastId, title) instead
     */
    @Deprecated
    public PaginatedResponse<Episode> searchByTitle(String title, Instant cursor, int limit) {
        return findAll(cursor, limit, null, null, title);
    }

    /**
     * @deprecated Use findAll(cursor, limit, isPublic, podcastId, title) instead
     */
    @Deprecated
    public PaginatedResponse<Episode> findPublicEpisodes(Instant cursor, int limit) {
        return findAll(cursor, limit, true, null, null);
    }

    public long countByPodcastId(String podcastId) {
        return episodeRepository.countByPodcastId(podcastId);
    }

    /**
     * Updates an existing episode
     * Validates that the user owns the podcast
     *
     * @param id ID of the episode to update
     * @param updated Updated episode data
     * @param userId ID of the user updating the episode
     * @return Updated episode
     * @throws ForbiddenException if user doesn't own the podcast
     */
    public Episode updateEpisode(String id, Episode updated, String userId) {
        Episode existing = episodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Episode not found: " + id));

        // Verify that user owns the podcast
        validatePodcastOwnership(existing.getPodcastId(), userId);

        updated.setId(id);
        updated.setPodcastId(existing.getPodcastId()); // Preserve podcast ID
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setUpdatedAt(Instant.now());
        Episode saved = episodeRepository.save(updated);
        log.info("Episode updated {} by user {}", id, userId);
        return saved;
    }

    /**
     * Partially updates an episode with only the provided fields.
     * Only the podcast owner can update episodes of their podcast.
     *
     * @param id Episode ID to update
     * @param patchRequest DTO with nullable fields to update
     * @param userId ID of user making the request (must be podcast owner)
     * @return Updated episode
     * @throws ResourceNotFoundException if episode not found
     * @throws ForbiddenException if user is not the podcast owner
     */
    public Episode patchEpisode(String id, EpisodePatchRequest patchRequest, String userId) {
        log.debug("Patching episode {} by user {}", id, userId);

        Episode existing = episodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Episode not found with id: " + id));

        // Verify podcast ownership
        validatePodcastOwnership(existing.getPodcastId(), userId);

        boolean changed = false;

        // Apply only non-null fields
        if (patchRequest.getTitle() != null) {
            existing.setTitle(patchRequest.getTitle());
            changed = true;
        }
        if (patchRequest.getSeason() != null) {
            existing.setSeason(patchRequest.getSeason());
            changed = true;
        }
        if (patchRequest.getNumber() != null) {
            existing.setNumber(patchRequest.getNumber());
            changed = true;
        }
        if (patchRequest.getDescription() != null) {
            existing.setDescription(patchRequest.getDescription());
            changed = true;
        }
        if (patchRequest.getTranscript() != null) {
            existing.setTranscript(patchRequest.getTranscript());
            changed = true;
        }
        if (patchRequest.getExplicit() != null) {
            existing.setExplicit(patchRequest.getExplicit());
            changed = true;
        }
        if (patchRequest.getIsPublic() != null) {
            existing.setIsPublic(patchRequest.getIsPublic());
            changed = true;
        }
        if (patchRequest.getPublishAt() != null) {
            existing.setPublishAt(patchRequest.getPublishAt());
            changed = true;
        }

        // Only update timestamp and save if something changed
        if (changed) {
            existing.setUpdatedAt(Instant.now());
            Episode saved = episodeRepository.save(existing);
            log.info("Episode {} patched successfully by user {}", id, userId);
            return saved;
        }

        log.debug("No changes to apply for episode {}", id);
        return existing;
    }

    /**
     * Deletes an episode
     * Validates that the user owns the podcast
     *
     * @param id ID of the episode to delete
     * @param userId ID of the user deleting the episode
     * @throws ForbiddenException if user doesn't own the podcast
     */
    public void deleteEpisode(String id, String userId) {
        Episode episode = episodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Episode not found: " + id));

        // Verify that user owns the podcast
        validatePodcastOwnership(episode.getPodcastId(), userId);

        episodeRepository.deleteById(id);
        log.info("Episode deleted {} by user {}", id, userId);
    }

    private PaginatedResponse<Episode> buildPaginatedResponse(List<Episode> episodes, int limit) {
        boolean hasMore = episodes.size() > limit;

        List<Episode> data;
        if (hasMore) {
            data = episodes.subList(0, limit);
        } else {
            data = episodes;
        }

        String nextCursor = null;
        if (hasMore) {
            if (!data.isEmpty()) {
                nextCursor = data.get(data.size() - 1).getCreatedAt().toString();
            }
        }

        return PaginatedResponse.<Episode>builder()
                .data(data)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .count(data.size())
                .build();
    }
}
