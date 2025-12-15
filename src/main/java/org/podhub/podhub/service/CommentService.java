package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.exception.ResourceNotFoundException;
import org.podhub.podhub.model.Comment;
import org.podhub.podhub.model.enums.CommentStatus;
import org.podhub.podhub.model.enums.CommentTargetType;
import org.podhub.podhub.repository.CommentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    public Comment createComment(Comment comment) {
        log.debug("Creating comment by user: {}", comment.getUserId());
        Instant now = Instant.now();
        comment.setCreatedAt(now);
        comment.setEditedAt(now);
        Comment saved = commentRepository.save(comment);
        log.info("Comment created with id {}", saved.getId());
        return saved;
    }

    public Optional<Comment> findById(String id) {
        return commentRepository.findById(id);
    }

    /**
     * Obtiene todos los comentarios con paginación cursor-based y filtros opcionales
     *
     * @param cursor Timestamp del último elemento (null para primera página)
     * @param limit Número máximo de elementos a retornar
     * @param podcastId Filtro opcional por podcast
     * @param episodeId Filtro opcional por episodio
     * @param parentId Filtro opcional por comentario padre (replies)
     * @param status Filtro opcional por estado del comentario
     * @return Respuesta paginada con cursor para siguiente página
     */
    public PaginatedResponse<Comment> findAll(Instant cursor, int limit, String podcastId, String episodeId, String parentId, String status) {
        log.debug("Finding comments with cursor: {}, limit: {}, podcastId: {}, episodeId: {}, parentId: {}, status: {}",
                  cursor, limit, podcastId, episodeId, parentId, status);

        List<Comment> comments;

        // Determinar qué método del repository usar según los filtros
        if (podcastId != null && !podcastId.trim().isEmpty()) {
            // Filtro por podcast
            if (cursor == null) {
                comments = commentRepository.findFirstCommentsByTarget(CommentTargetType.PODCAST, podcastId, limit + 1);
            } else {
                comments = commentRepository.findNextCommentsByTarget(CommentTargetType.PODCAST, podcastId, cursor, limit + 1);
            }
        } else if (episodeId != null && !episodeId.trim().isEmpty()) {
            // Filtro por episodio
            if (cursor == null) {
                comments = commentRepository.findFirstCommentsByTarget(CommentTargetType.EPISODE, episodeId, limit + 1);
            } else {
                comments = commentRepository.findNextCommentsByTarget(CommentTargetType.EPISODE, episodeId, cursor, limit + 1);
            }
        } else if (parentId != null && !parentId.trim().isEmpty()) {
            // Filtro por padre (replies)
            if (cursor == null) {
                comments = commentRepository.findFirstCommentsByParent(parentId, limit + 1);
            } else {
                comments = commentRepository.findNextCommentsByParent(parentId, cursor, limit + 1);
            }
        } else if (status != null && !status.trim().isEmpty()) {
            // Filtro por estado
            CommentStatus commentStatus = CommentStatus.valueOf(status.toUpperCase());
            if (cursor == null) {
                comments = commentRepository.findFirstCommentsByStatus(commentStatus, limit + 1);
            } else {
                comments = commentRepository.findNextCommentsByStatus(commentStatus, cursor, limit + 1);
            }
        } else {
            // Sin filtros, no tiene sentido listar todos los comentarios sin filtro
            // Retornar lista vacía
            comments = List.of();
        }

        return buildPaginatedResponse(comments, limit);
    }

    /**
     * @deprecated Use findAll(cursor, limit, null, null, null, status) instead
     */
    @Deprecated
    public PaginatedResponse<Comment> findByStatus(CommentStatus status, Instant cursor, int limit) {
        return findAll(cursor, limit, null, null, null, status.name());
    }

    /**
     * @deprecated Use findAll(cursor, limit, null, null, parentId, null) instead
     */
    @Deprecated
    public PaginatedResponse<Comment> findByParent(String parentId, Instant cursor, int limit) {
        return findAll(cursor, limit, null, null, parentId, null);
    }

    /**
     * @deprecated Use findAll(cursor, limit, podcastId, episodeId, null, null) instead
     */
    @Deprecated
    public PaginatedResponse<Comment> findByTarget(CommentTargetType type, String targetId, Instant cursor, int limit) {
        if (type == CommentTargetType.PODCAST) {
            return findAll(cursor, limit, targetId, null, null, null);
        } else {
            return findAll(cursor, limit, null, targetId, null, null);
        }
    }

    /**
     * @deprecated Use findAll instead
     */
    @Deprecated
    public PaginatedResponse<Comment> findByTargetId(String targetId, Instant cursor, int limit) {
        return findAll(cursor, limit, null, targetId, null, null);
    }

    /**
     * @deprecated Use findAll instead
     */
    @Deprecated
    public PaginatedResponse<Comment> findThread(String targetId, Instant cursor, int limit) {
        return findAll(cursor, limit, null, targetId, null, null);
    }

    /**
     * @deprecated Use findAll instead
     */
    @Deprecated
    public PaginatedResponse<Comment> findByUserId(String userId, Instant cursor, int limit) {
        log.debug("Finding comments by user: {} with cursor: {} and limit: {}", userId, cursor, limit);

        List<Comment> comments;
        if (cursor == null) {
            comments = commentRepository.findFirstCommentsByUser(userId, limit + 1);
        } else {
            comments = commentRepository.findNextCommentsByUser(userId, cursor, limit + 1);
        }

        return buildPaginatedResponse(comments, limit);
    }

    public long countByTargetId(String targetId) {
        return commentRepository.countByTargetId(targetId);
    }

    public Comment updateComment(String id, Comment updated) {
        Comment existing = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + id));
        updated.setId(id);
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setEditedAt(Instant.now());
        Comment saved = commentRepository.save(updated);
        log.info("Comment updated {}", id);
        return saved;
    }

    public void deleteComment(String id) {
        if (!commentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Comment not found: " + id);
        }
        commentRepository.deleteById(id);
        log.info("Comment deleted {}", id);
    }

    private PaginatedResponse<Comment> buildPaginatedResponse(List<Comment> comments, int limit) {
        boolean hasMore = comments.size() > limit;

        List<Comment> data;
        if (hasMore) {
            data = comments.subList(0, limit);
        } else {
            data = comments;
        }

        String nextCursor = null;
        if (hasMore) {
            if (!data.isEmpty()) {
                nextCursor = data.get(data.size() - 1).getCreatedAt().toString();
            }
        }

        return PaginatedResponse.<Comment>builder()
                .data(data)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .count(data.size())
                .build();
    }
}
