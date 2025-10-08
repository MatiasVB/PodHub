package org.podhub.podhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.podhub.podhub.model.Comment;
import org.podhub.podhub.model.enums.CommentStatus;
import org.podhub.podhub.model.enums.CommentTargetType;
import org.podhub.podhub.repository.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    public Page<Comment> findByUserId(String userId, Pageable pageable) {
        return commentRepository.findByUserId(userId, pageable);
    }

    public Page<Comment> findByTargetId(String targetId, Pageable pageable) {
        return commentRepository.findByTargetId(targetId, pageable);
    }

    public Page<Comment> findByTarget(CommentTargetType type, String targetId, Pageable pageable) {
        return commentRepository.findByTargetTypeAndTargetId(type, targetId, pageable);
    }

    public Page<Comment> findThread(String targetId, Pageable pageable) {
        return commentRepository.findByTargetIdAndParentIdIsNull(targetId, pageable);
    }

    public Page<Comment> findByParent(String parentId, Pageable pageable) {
        return commentRepository.findByParentId(parentId, pageable);
    }

    public Page<Comment> findByStatus(CommentStatus status, Pageable pageable) {
        return commentRepository.findByStatus(status, pageable);
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
}
