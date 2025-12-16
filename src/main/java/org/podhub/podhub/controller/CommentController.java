package org.podhub.podhub.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.podhub.podhub.dto.PaginatedResponse;
import org.podhub.podhub.exception.BadRequestException;
import org.podhub.podhub.exception.ResourceNotFoundException;
import org.podhub.podhub.model.Comment;
import org.podhub.podhub.model.User;
import org.podhub.podhub.model.enums.CommentStatus;
import org.podhub.podhub.model.enums.CommentTargetType;
import org.podhub.podhub.repository.UserRepository;
import org.podhub.podhub.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;

    /**
     * POST /api/comments
     * Crea un nuevo comentario
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
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
     * Solo el propietario del comentario puede editarlo
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Comment> updateComment(
            @PathVariable String id,
            @Valid @RequestBody Comment comment,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String userId = user.getId();
        Comment updated = commentService.updateComment(id, comment, userId);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/comments/{id}
     * Elimina un comentario por ID
     * Propietario del comentario o creador del podcast/episodio pueden eliminar
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String userId = user.getId();
        commentService.deleteComment(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/comments?cursor={timestamp}&limit={number}&podcastId={id}&episodeId={id}&parentId={id}&status={status}
     * Lista comentarios con filtros opcionales y paginación cursor-based
     *
     * Ejemplos:
     * - GET /api/comments?podcastId={id}                 (comentarios de podcast)
     * - GET /api/comments?episodeId={id}                 (comentarios de episodio)
     * - GET /api/comments?parentId={id}                  (respuestas a comentario)
     * - GET /api/comments?status=PENDING                 (por estado - requiere ADMIN)
     * - GET /api/comments?cursor={timestamp}&limit=20    (siguiente página)
     *
     * Nota: Al menos un filtro es requerido (no se puede listar todos los comentarios sin filtro)
     */
    @GetMapping
    public ResponseEntity<PaginatedResponse<Comment>> getAllComments(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String podcastId,
            @RequestParam(required = false) String episodeId,
            @RequestParam(required = false) String parentId,
            @RequestParam(required = false) String status) {
        Instant cursorInstant = cursor != null ? Instant.parse(cursor) : null;

        // Validar status si se proporciona
        if (status != null && !status.trim().isEmpty()) {
            try {
                CommentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Valor de 'status' inválido: " + status);
            }
        }

        PaginatedResponse<Comment> response = commentService.findAll(cursorInstant, limit, podcastId, episodeId, parentId, status);
        return ResponseEntity.ok(response);
    }
}
