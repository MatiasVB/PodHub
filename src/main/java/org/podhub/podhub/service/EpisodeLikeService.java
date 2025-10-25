package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.exception.ConflictException;
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

    private static final int DEFAULT_MAX_LIMIT = 100;

    private final EpisodeLikeRepository episodeLikeRepository;

    public EpisodeLike like(String userId, String episodeId) {
        ensureNotBlank(userId, "userId");
        ensureNotBlank(episodeId, "episodeId");

        log.debug("User {} liking episode {}", userId, episodeId);

        if (episodeLikeRepository.existsByUserIdAndEpisodeId(userId, episodeId)) {
            throw new ConflictException("Ya existe un like para este episodio por este usuario");
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
        ensureNotBlank(userId, "userId");
        ensureNotBlank(episodeId, "episodeId");

        log.debug("User {} unliking episode {}", userId, episodeId);

        EpisodeLike existing = episodeLikeRepository.findByUserIdAndEpisodeId(userId,episodeId)
                .orElseThrow(() -> new IllegalArgumentException("No existe un like para este episodio por este usuario"));

        episodeLikeRepository.delete(existing);
        log.info("Like deleted (user {}, episode {})", userId, episodeId);
    }

    public Optional<EpisodeLike> findById(String id) {
        return episodeLikeRepository.findById(id);
    }

    /**
     * Obtiene los likes de un episodio con paginación cursor-based
     *
     * @param episodeId Episodio del que se obtienen los likes
     * @param cursor    Timestamp del último elemento (null para primera página)
     * @param limit     Número máximo de elementos a retornar
     * @return Respuesta paginada con cursor para siguiente página
     */
    public PaginatedResponse<EpisodeLike> findByEpisodeId(String episodeId, Instant cursor, int limit) {
        ensureNotBlank(episodeId, "episodeId");
        validateLimit(limit);

        log.debug("Finding likes by episode: {} with cursor: {} and limit: {}", episodeId, cursor, limit);

        List<EpisodeLike> likes;
        if (cursor == null) {
            likes = episodeLikeRepository.findFirstLikesByEpisode(episodeId, limit + 1);
        } else {
            likes = episodeLikeRepository.findNextLikesByEpisode(episodeId, cursor, limit + 1);
        }

        return buildPaginatedResponse(likes, limit);
    }

    /**
     * Obtiene los likes de un usuario con paginación cursor-based
     */
    public PaginatedResponse<EpisodeLike> findByUserId(String userId, Instant cursor, int limit) {
        ensureNotBlank(userId, "userId");
        validateLimit(limit);

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
        ensureNotBlank(episodeId, "episodeId");
        return episodeLikeRepository.countByEpisodeId(episodeId);
    }

    /**
     * Construye la respuesta paginada a partir de una lista de likes
     *
     * @param likes Lista con limit+1 elementos
     * @param limit Límite real solicitado
     * @return PaginatedResponse con nextCursor si hay más elementos
     */
    private PaginatedResponse<EpisodeLike> buildPaginatedResponse(List<EpisodeLike> likes, int limit) {
        boolean hasMore = likes.size() > limit;

        // Si hay más elementos, solo retornamos los primeros 'limit'
        List<EpisodeLike> data;
        if (hasMore) {
            data = likes.subList(0, limit);
        } else {
            data = likes;
        }

        // Calcular el nextCursor (createdAt del último elemento)
        String nextCursor = null;
        if (hasMore && !data.isEmpty()) {
            nextCursor = data.get(data.size() - 1).getCreatedAt().toString();
        }

        return PaginatedResponse.<EpisodeLike>builder()
                .data(data)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .count(data.size())
                .build();
    }

    /* ======================= Helpers ======================= */

    private void ensureNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " no puede estar vacío");
        }
    }

    private void validateLimit(int limit) {
        if (limit <= 0 || limit > DEFAULT_MAX_LIMIT) {
            throw new IllegalArgumentException("El límite debe estar entre 1 y " + DEFAULT_MAX_LIMIT);
        }
    }
}
