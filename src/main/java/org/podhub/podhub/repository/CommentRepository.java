package org.podhub.podhub.repository;

import org.podhub.podhub.model.Comment;
import org.podhub.podhub.model.enums.CommentStatus;
import org.podhub.podhub.model.enums.CommentTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {

    Page<Comment> findByUserId(String userId, Pageable pageable);

    // Campos anidados del embebido CommentTarget: target.id / target.type
    Page<Comment> findByTargetId(String targetId, Pageable pageable);
    Page<Comment> findByTargetTypeAndTargetId(CommentTargetType type, String targetId, Pageable pageable);

    Page<Comment> findByParentId(String parentId, Pageable pageable);
    Page<Comment> findByTargetIdAndParentIdIsNull(String targetId, Pageable pageable);

    Page<Comment> findByStatus(CommentStatus status, Pageable pageable);

    long countByTargetId(String targetId);
}

