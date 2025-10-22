package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.dto.PaginatedResponse;
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
     * Obtiene los comentarios de un usuario con paginación cursor-based
     *
     * @param userId Usuario que hizo los comentarios
     * @param cursor Timestamp del último elemento (null para primera página)
     * @param limit  Número máximo de elementos a retornar
     * @return Respuesta paginada con cursor para siguiente página
     */
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

    /**
     * Obtiene los comentarios de un target específico con paginación cursor-based
     */
    public PaginatedResponse<Comment> findByTargetId(String targetId, Instant cursor, int limit) {
        log.debug("Finding comments by target: {} with cursor: {} and limit: {}", targetId, cursor, limit);

        List<Comment> comments;
        if (cursor == null) {
            comments = commentRepository.findFirstCommentsByTargetId(targetId, limit + 1);
        } else {
            comments = commentRepository.findNextCommentsByTargetId(targetId, cursor, limit + 1);
        }

        return buildPaginatedResponse(comments, limit);
    }

    /**
     * Obtiene los comentarios de un target específico filtrado por tipo con paginación cursor-based
     */
    public PaginatedResponse<Comment> findByTarget(CommentTargetType type, String targetId, Instant cursor, int limit) {
        log.debug("Finding comments by target type: {} and id: {} with cursor: {} and limit: {}", type, targetId, cursor, limit);

        List<Comment> comments;
        if (cursor == null) {
            comments = commentRepository.findFirstCommentsByTarget(type, targetId, limit + 1);
        } else {
            comments = commentRepository.findNextCommentsByTarget(type, targetId, cursor, limit + 1);
        }

        return buildPaginatedResponse(comments, limit);
    }

    /**
     * Obtiene los comentarios raíz (sin padre) de un target con paginación cursor-based
     */
    public PaginatedResponse<Comment> findThread(String targetId, Instant cursor, int limit) {
        log.debug("Finding thread comments for target: {} with cursor: {} and limit: {}", targetId, cursor, limit);

        List<Comment> comments;
        if (cursor == null) {
            comments = commentRepository.findFirstThreadComments(targetId, limit + 1);
        } else {
            comments = commentRepository.findNextThreadComments(targetId, cursor, limit + 1);
        }

        return buildPaginatedResponse(comments, limit);
    }

    /**
     * Obtiene las respuestas a un comentario específico con paginación cursor-based
     */
    public PaginatedResponse<Comment> findByParent(String parentId, Instant cursor, int limit) {
        log.debug("Finding replies for comment: {} with cursor: {} and limit: {}", parentId, cursor, limit);

        List<Comment> comments;
        if (cursor == null) {
            comments = commentRepository.findFirstCommentsByParent(parentId, limit + 1);
        } else {
            comments = commentRepository.findNextCommentsByParent(parentId, cursor, limit + 1);
        }

        return buildPaginatedResponse(comments, limit);
    }

    /**
     * Obtiene los comentarios por estado con paginación cursor-based
     */
    public PaginatedResponse<Comment> findByStatus(CommentStatus status, Instant cursor, int limit) {
        log.debug("Finding comments by status: {} with cursor: {} and limit: {}", status, cursor, limit);

        List<Comment> comments;
        if (cursor == null) {
            comments = commentRepository.findFirstCommentsByStatus(status, limit + 1);
        } else {
            comments = commentRepository.findNextCommentsByStatus(status, cursor, limit + 1);
        }

        return buildPaginatedResponse(comments, limit);
    }

    public long countByTargetId(String targetId) {
        return commentRepository.countByTargetId(targetId);
    }

    public Comment updateComment(String id, Comment updated) {
        Comment existing = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + id));
        updated.setId(id);
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setEditedAt(Instant.now());
        Comment saved = commentRepository.save(updated);
        log.info("Comment updated {}", id);
        return saved;
    }

    public void deleteComment(String id) {
        if (!commentRepository.existsById(id)) {
            throw new IllegalArgumentException("Comment not found: " + id);
        }
        commentRepository.deleteById(id);
        log.info("Comment deleted {}", id);
    }

    /**
     * Construye la respuesta paginada a partir de una lista de comentarios
     *
     * @param comments Lista con limit+1 elementos
     * @param limit    Límite real solicitado
     * @return PaginatedResponse con nextCursor si hay más elementos
     */
    private PaginatedResponse<Comment> buildPaginatedResponse(List<Comment> comments, int limit) {
        boolean hasMore = comments.size() > limit;

        // Si hay más elementos, solo retornamos los primeros 'limit'
        List<Comment> data;
        if (hasMore) {
            data = comments.subList(0, limit);
        } else {
            data = comments;
        }

        // Calcular el nextCursor (createdAt del último elemento)
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
