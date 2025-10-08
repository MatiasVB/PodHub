package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.model.ListeningProgress;
import org.podhub.podhub.repository.ListeningProgressRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListeningProgressService {

    private final ListeningProgressRepository listeningProgressRepository;

    public ListeningProgress upsert(String userId, String episodeId, Integer positionSeconds) {
        log.debug("Upsert progress user={}, episode={}, pos={}s", userId, episodeId, positionSeconds);
        ListeningProgress progress = listeningProgressRepository
                .findByUserIdAndEpisodeId(userId, episodeId)
                .orElse(ListeningProgress.builder()
                        .userId(userId)
                        .episodeId(episodeId)
                        .createdAt(Instant.now())
                        .build());

        progress.setPositionSeconds(positionSeconds);
        progress.setCreatedAt(Instant.now());
        ListeningProgress saved = listeningProgressRepository.save(progress);
        log.info("Progress saved {}", saved.getId());
        return saved;
    }

    public Optional<ListeningProgress> findById(String id) {
        return listeningProgressRepository.findById(id);
    }

    public Optional<ListeningProgress> findByUserAndEpisode(String userId, String episodeId) {
        return listeningProgressRepository.findByUserIdAndEpisodeId(userId, episodeId);
    }

    public Page<ListeningProgress> findByUserId(String userId, Pageable pageable) {
        return listeningProgressRepository.findByUserId(userId, pageable);
    }

    public Page<ListeningProgress> findByEpisodeId(String episodeId, Pageable pageable) {
        return listeningProgressRepository.findByEpisodeId(episodeId, pageable);
    }

    public void deleteById(String id) {
        if (!listeningProgressRepository.existsById(id)) {
            throw new IllegalArgumentException("ListeningProgress not found: " + id);
        }
        listeningProgressRepository.deleteById(id);
        log.info("Progress deleted {}", id);
    }
}
