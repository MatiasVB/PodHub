package org.podhub.podhub.repository;

import org.podhub.podhub.model.Comment;
import org.podhub.podhub.model.enums.CommentStatus;
import org.podhub.podhub.model.enums.CommentTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

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

    // ========== Paginación cursor-based: Por usuario ==========

    @Query(value = "{ 'userId': ?0 }", sort = "{ 'createdAt': -1 }")
    List<Comment> findFirstCommentsByUser(String userId, int limit);

    @Query(value = "{ 'userId': ?0, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<Comment> findNextCommentsByUser(String userId, Instant cursor, int limit);

    // ========== Paginación cursor-based: Por target ID ==========

    @Query(value = "{ 'target.id': ?0 }", sort = "{ 'createdAt': -1 }")
    List<Comment> findFirstCommentsByTargetId(String targetId, int limit);

    @Query(value = "{ 'target.id': ?0, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<Comment> findNextCommentsByTargetId(String targetId, Instant cursor, int limit);

    // ========== Paginación cursor-based: Por target tipo y ID ==========

    @Query(value = "{ 'target.type': ?0, 'target.id': ?1 }", sort = "{ 'createdAt': -1 }")
    List<Comment> findFirstCommentsByTarget(CommentTargetType type, String targetId, int limit);

    @Query(value = "{ 'target.type': ?0, 'target.id': ?1, 'createdAt': { $lt: ?2 } }", sort = "{ 'createdAt': -1 }")
    List<Comment> findNextCommentsByTarget(CommentTargetType type, String targetId, Instant cursor, int limit);

    // ========== Paginación cursor-based: Thread raíz (sin padre) ==========

    @Query(value = "{ 'target.id': ?0, 'parentId': null }", sort = "{ 'createdAt': -1 }")
    List<Comment> findFirstThreadComments(String targetId, int limit);

    @Query(value = "{ 'target.id': ?0, 'parentId': null, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<Comment> findNextThreadComments(String targetId, Instant cursor, int limit);

    // ========== Paginación cursor-based: Respuestas a comentario ==========

    @Query(value = "{ 'parentId': ?0 }", sort = "{ 'createdAt': -1 }")
    List<Comment> findFirstCommentsByParent(String parentId, int limit);

    @Query(value = "{ 'parentId': ?0, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<Comment> findNextCommentsByParent(String parentId, Instant cursor, int limit);

    // ========== Paginación cursor-based: Por estado ==========

    @Query(value = "{ 'status': ?0 }", sort = "{ 'createdAt': -1 }")
    List<Comment> findFirstCommentsByStatus(CommentStatus status, int limit);

    @Query(value = "{ 'status': ?0, 'createdAt': { $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<Comment> findNextCommentsByStatus(CommentStatus status, Instant cursor, int limit);
}

