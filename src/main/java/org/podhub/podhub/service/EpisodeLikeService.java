package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.exception.ConflictException;
import org.podhub.podhub.exception.ResourceNotFoundException;
import org.podhub.podhub.model.EpisodeLike;
import org.podhub.podhub.repository.EpisodeLikeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeLikeService {

    private final EpisodeLikeRepository episodeLikeRepository;

    public EpisodeLike like(String userId, String episodeId) {
        log.debug("User {} liking episode {}", userId, episodeId);
        if (episodeLikeRepository.existsByUserIdAndEpisodeId(userId, episodeId)) {
            throw new ConflictException("Like already exists");
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
        EpisodeLike toDelete = existing.orElseThrow(() ->
                new ResourceNotFoundException("Like not found for user and episode"));
        episodeLikeRepository.delete(toDelete);
    }

    public boolean exists(String userId, String episodeId) {
        log.debug("Checking if like exists for user={}, episode={}", userId, episodeId);
        boolean exists = episodeLikeRepository.existsByUserIdAndEpisodeId(userId, episodeId);
        log.info("Like exists for user={}, episode={} => {}", userId, episodeId, exists);
        return exists;
    }

    public Optional<EpisodeLike> findById(String id) {
        return episodeLikeRepository.findById(id);
    }

    public PaginatedResponse<EpisodeLike> findByEpisodeId(String episodeId, Instant cursor, int limit) {
        log.debug("Finding likes by episode: {} with cursor: {} and limit: {}", episodeId, cursor, limit);

        List<EpisodeLike> likes;
        if (cursor == null) {
            likes = episodeLikeRepository.findFirstLikesByEpisode(episodeId, limit + 1);
        } else {
            likes = episodeLikeRepository.findNextLikesByEpisode(episodeId, cursor, limit + 1);
        }

        return buildPaginatedResponse(likes, limit);
    }

    public PaginatedResponse<EpisodeLike> findByUserId(String userId, Instant cursor, int limit) {
        log.debug("Finding likes by user: {} with cursor: {} and limit: {}", userId, cursor, limit);

        List<EpisodeLike> likes;
        if (cursor == null) {
            likes = episodeLikeRepository.findFirstLikesByUser(userId, limit + 1);
        } else {
            likes = episodeLikeRepository.findNextLikesByUser(userId, cursor, limit + 1);
        }

        return buildPaginatedResponse(likes, limit);
    }

    public long countByEpisodeId(String episodeId) {
        return episodeLikeRepository.countByEpisodeId(episodeId);
    }

    private PaginatedResponse<EpisodeLike> buildPaginatedResponse(List<EpisodeLike> likes, int limit) {
        boolean hasMore = likes.size() > limit;

        List<EpisodeLike> data;
        if (hasMore) {
            data = likes.subList(0, limit);
        } else {
            data = likes;
        }

        String nextCursor = null;
        if (hasMore) {
            if (!data.isEmpty()) {
                nextCursor = data.get(data.size() - 1).getCreatedAt().toString();
            }
        }

        return PaginatedResponse.<EpisodeLike>builder()
                .data(data)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .count(data.size())
                .build();
    }
}
