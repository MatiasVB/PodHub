package org.podhub.podhub.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.exception.BadRequestException;
import org.podhub.podhub.model.Comment;
import org.podhub.podhub.model.enums.CommentStatus;
import org.podhub.podhub.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * POST /api/comments
     * Crea un nuevo comentario
     */
    @PostMapping
    public ResponseEntity<Comment> createComment(@Valid @RequestBody Comment comment) {
        Comment created = commentService.createComment(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/comments/{id}
     * Obtiene un comentario por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Comment> getCommentById(@PathVariable String id) {
        return commentService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/comments/{id}
     * Actualiza un comentario existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<Comment> updateComment(
            @PathVariable String id,
            @Valid @RequestBody Comment comment) {
        try {
            Comment updated = commentService.updateComment(id, comment);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/comments/{id}
     * Elimina un comentario por ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable String id) {
        try {
            commentService.deleteComment(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/comments/podcast/{podcastId}?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista comentarios de un podcast específico con paginación cursor-based
     */
    @GetMapping("/podcast/{podcastId}")
    public ResponseEntity<PaginatedResponse<Comment>> getCommentsByPodcast(
            @PathVariable String podcastId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<Comment> response = commentService.findByPodcastId(podcastId, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/comments/episode/{episodeId}?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista comentarios de un episodio específico con paginación cursor-based
     */
    @GetMapping("/episode/{episodeId}")
    public ResponseEntity<PaginatedResponse<Comment>> getCommentsByEpisode(
            @PathVariable String episodeId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<Comment> response = commentService.findByEpisodeId(episodeId, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/comments/parent/{parentId}?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista respuestas (comentarios hijos) de un comentario padre con paginación cursor-based
     */
    @GetMapping("/parent/{parentId}")
    public ResponseEntity<PaginatedResponse<Comment>> getCommentsByParent(
            @PathVariable String parentId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
        PaginatedResponse<Comment> response = commentService.findByParent(parentId, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/comments/status/{status}?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista comentarios por estado (PENDING/APPROVED/REJECTED) con paginación cursor-based
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<PaginatedResponse<Comment>> getCommentsByStatus(
            @PathVariable String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;

        // Convertir String -> Enum y delegar el 400 a tu GlobalExceptionHandler
        final CommentStatus statusEnum;
        try {
            statusEnum = CommentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Valor de 'status' inválido: " + status);
        }

        PaginatedResponse<Comment> response = commentService.findByStatus(statusEnum, cursorInstant, limit);
        return ResponseEntity.ok(response);
    }


    /**
     * GET /api/comments?cursor=2024-01-15T10:30:00Z&limit=20
     * Lista todos los comentarios con paginación cursor-based
     *
     * Primera página: GET /api/comments?limit=20
     * Siguiente página: GET /api/comments?cursor={nextCursor}&limit=20
     *
     @GetMapping
     public ResponseEntity<PaginatedResponse<Comment>> getAllComments(
     @RequestParam(required = false) String cursor,
     @RequestParam(defaultValue = "20") int limit) {
     Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;
     PaginatedResponse<Comment> response = commentService.findAll(cursorInstant, limit);
     return ResponseEntity.ok(response);
     }
     */
}
