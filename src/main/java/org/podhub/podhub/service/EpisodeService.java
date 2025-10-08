package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.model.Episode;
import org.podhub.podhub.repository.EpisodeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    public Page<Episode> findAll(Pageable pageable) {
        return episodeRepository.findAll(pageable);
    }

    public Page<Episode> findByPodcastId(String podcastId, Pageable pageable) {
        return episodeRepository.findByPodcastId(podcastId, pageable);
    }

    public Page<Episode> searchByTitle(String title, Pageable pageable) {
        return episodeRepository.findByTitleContainingIgnoreCase(title, pageable);
    }

    public Page<Episode> findPublicEpisodes(Pageable pageable) {
        return episodeRepository.findByIsPublicTrue(pageable);
    }

    public long countByPodcastId(String podcastId) {
        return episodeRepository.countByPodcastId(podcastId);
    }

    public Episode updateEpisode(String id, Episode updated) {
        Episode existing = episodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Episode not found: " + id));
        updated.setId(id);
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setUpdatedAt(Instant.now());
        Episode saved = episodeRepository.save(updated);
        log.info("Episode updated {}", id);
        return saved;
    }

    public void deleteEpisode(String id) {
        if (!episodeRepository.existsById(id)) {
            throw new IllegalArgumentException("Episode not found: " + id);
        }
        episodeRepository.deleteById(id);
        log.info("Episode deleted {}", id);
    }
}
