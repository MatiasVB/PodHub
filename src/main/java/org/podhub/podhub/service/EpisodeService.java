package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.exception.ResourceNotFoundException;
import org.podhub.podhub.model.Episode;
import org.podhub.podhub.repository.EpisodeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeService {

    private final EpisodeRepository episodeRepository;

    public Episode createEpisode(Episode episode) {
        log.debug("Creating episode '{}'", episode.getTitle());
        Instant now = Instant.now();
        episode.setCreatedAt(now);
        episode.setUpdatedAt(now);
        if (episode.getIsPublic() == null) {
            episode.setIsPublic(false);
        }
        Episode saved = episodeRepository.save(episode);
        log.info("Episode created {}", saved.getId());
        return saved;
    }

    public Optional<Episode> findById(String id) {
        return episodeRepository.findById(id);
    }

    public PaginatedResponse<Episode> findAll(Instant cursor, int limit) {
        log.debug("Finding all episodes with cursor: {} and limit: {}", cursor, limit);

        List<Episode> episodes;
        if (cursor == null) {
            episodes = episodeRepository.findFirstEpisodes(limit + 1);
        } else {
            episodes = episodeRepository.findNextEpisodes(cursor, limit + 1);
        }

        return buildPaginatedResponse(episodes, limit);
    }

    public PaginatedResponse<Episode> findByPodcastId(String podcastId, Instant cursor, int limit) {
        log.debug("Finding episodes by podcast: {} with cursor: {} and limit: {}", podcastId, cursor, limit);

        List<Episode> episodes;
        if (cursor == null) {
            episodes = episodeRepository.findFirstEpisodesByPodcast(podcastId, limit + 1);
        } else {
            episodes = episodeRepository.findNextEpisodesByPodcast(podcastId, cursor, limit + 1);
        }

        return buildPaginatedResponse(episodes, limit);
    }

    public PaginatedResponse<Episode> searchByTitle(String title, Instant cursor, int limit) {
        log.debug("Searching episodes by title: {} with cursor: {} and limit: {}", title, cursor, limit);

        List<Episode> episodes;
        if (cursor == null) {
            episodes = episodeRepository.findFirstEpisodesByTitle(title, limit + 1);
        } else {
            episodes = episodeRepository.findNextEpisodesByTitle(title, cursor, limit + 1);
        }

        return buildPaginatedResponse(episodes, limit);
    }

    public PaginatedResponse<Episode> findPublicEpisodes(Instant cursor, int limit) {
        log.debug("Finding public episodes with cursor: {} and limit: {}", cursor, limit);

        List<Episode> episodes;
        if (cursor == null) {
            episodes = episodeRepository.findFirstPublicEpisodes(limit + 1);
        } else {
            episodes = episodeRepository.findNextPublicEpisodes(cursor, limit + 1);
        }

        return buildPaginatedResponse(episodes, limit);
    }

    public long countByPodcastId(String podcastId) {
        return episodeRepository.countByPodcastId(podcastId);
    }

    public Episode updateEpisode(String id, Episode updated) {
        Episode existing = episodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Episode not found: " + id));
        updated.setId(id);
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setUpdatedAt(Instant.now());
        Episode saved = episodeRepository.save(updated);
        log.info("Episode updated {}", id);
        return saved;
    }

    public void deleteEpisode(String id) {
        if (!episodeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Episode not found: " + id);
        }
        episodeRepository.deleteById(id);
        log.info("Episode deleted {}", id);
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
