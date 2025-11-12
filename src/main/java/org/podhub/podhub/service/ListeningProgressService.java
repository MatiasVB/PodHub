package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.exception.ResourceNotFoundException;
import org.podhub.podhub.model.ListeningProgress;
import org.podhub.podhub.repository.ListeningProgressRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListeningProgressService {

    private final ListeningProgressRepository listeningProgressRepository;

    public ListeningProgress upsert(String userId, String episodeId, int positionSeconds, boolean completed) {
        log.debug("Upsert progress user={}, episode={}, pos={}s, completed={}", userId, episodeId, positionSeconds, completed);

        ListeningProgress progress = listeningProgressRepository
                .findByUserIdAndEpisodeId(userId, episodeId)
                .orElse(ListeningProgress.builder()
                        .userId(userId)
                        .episodeId(episodeId)
                        .createdAt(Instant.now())
                        .build());

        progress.setPositionSeconds(positionSeconds);
        progress.setCompleted(completed);
        progress.setUpdatedAt(Instant.now());

        ListeningProgress saved = listeningProgressRepository.save(progress);

        log.info("Progress saved {} (completed={})", saved.getId(), saved.getCompleted());
        return saved;
    }


    public Optional<ListeningProgress> findById(String id) {
        return listeningProgressRepository.findById(id);
    }

    public Optional<ListeningProgress> findByUserAndEpisode(String userId, String episodeId) {
        return listeningProgressRepository.findByUserIdAndEpisodeId(userId, episodeId);
    }

    public PaginatedResponse<ListeningProgress> findByUserId(String userId, Instant cursor, int limit) {
        log.debug("Finding listening progress by user: {} with cursor: {} and limit: {}", userId, cursor, limit);

        List<ListeningProgress> progressList;
        if (cursor == null) {
            progressList = listeningProgressRepository.findFirstProgressByUser(userId, limit + 1);
        } else {
            progressList = listeningProgressRepository.findNextProgressByUser(userId, cursor, limit + 1);
        }

        return buildPaginatedResponse(progressList, limit);
    }

    public PaginatedResponse<ListeningProgress> findByEpisodeId(String episodeId, Instant cursor, int limit) {
        log.debug("Finding listening progress by episode: {} with cursor: {} and limit: {}", episodeId, cursor, limit);

        List<ListeningProgress> progressList;
        if (cursor == null) {
            progressList = listeningProgressRepository.findFirstProgressByEpisode(episodeId, limit + 1);
        } else {
            progressList = listeningProgressRepository.findNextProgressByEpisode(episodeId, cursor, limit + 1);
        }

        return buildPaginatedResponse(progressList, limit);
    }

    public void delete(String userId, String episodeId) {
        log.debug("Deleting progress for user={}, episode={}", userId, episodeId);

        var progress = listeningProgressRepository
                .findByUserIdAndEpisodeId(userId, episodeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("ListeningProgress not found for user=" + userId + ", episode=" + episodeId)
                );

        listeningProgressRepository.delete(progress);
        log.info("Progress deleted for user={}, episode={}", userId, episodeId);
    }


    private PaginatedResponse<ListeningProgress> buildPaginatedResponse(List<ListeningProgress> progressList, int limit) {
        boolean hasMore = progressList.size() > limit;

        List<ListeningProgress> data;
        if (hasMore) {
            data = progressList.subList(0, limit);
        } else {
            data = progressList;
        }

        String nextCursor = null;
        if (hasMore) {
            if (!data.isEmpty()) {
                nextCursor = data.get(data.size() - 1).getCreatedAt().toString();
            }
        }

        return PaginatedResponse.<ListeningProgress>builder()
                .data(data)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .count(data.size())
                .build();
    }
}
