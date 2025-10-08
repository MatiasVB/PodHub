package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.model.EpisodeLike;
import org.podhub.podhub.repository.EpisodeLikeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeLikeService {

    private final EpisodeLikeRepository episodeLikeRepository;

    public EpisodeLike like(String userId, String episodeId) {
        log.debug("User {} liking episode {}", userId, episodeId);
        if (episodeLikeRepository.existsByUserIdAndEpisodeId(userId, episodeId)) {
            throw new IllegalArgumentException("Like already exists");
        }
        EpisodeLike like = EpisodeLike.builder()
                .userId(userId)
                .episodeId(episodeId)
                .createdAt(Instant.now())
                .build();
        EpisodeLike saved = episodeLikeRepository.save(like);
        log.info("Like created {}", saved.getId());
        return saved;
    }

    public void unlike(String userId, String episodeId) {
        log.debug("User {} unliking episode {}", userId, episodeId);
        Optional<EpisodeLike> existing = episodeLikeRepository.findByUserIdAndEpisodeId(userId, episodeId);
        existing.ifPresent(episodeLikeRepository::delete);
    }

    public Optional<EpisodeLike> findById(String id) {
        return episodeLikeRepository.findById(id);
    }

    public Page<EpisodeLike> findByEpisodeId(String episodeId, Pageable pageable) {
        return episodeLikeRepository.findByEpisodeId(episodeId, pageable);
    }

    public Page<EpisodeLike> findByUserId(String userId, Pageable pageable) {
        return episodeLikeRepository.findByUserId(userId, pageable);
    }

    public long countByEpisodeId(String episodeId) {
        return episodeLikeRepository.countByEpisodeId(episodeId);
    }
}
