package org.podhub.podhub.repository;

import org.podhub.podhub.model.Podcast;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PodcastRepository extends MongoRepository<Podcast, String> {

    Optional<Podcast> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Podcast> findByCreatorId(String creatorId);

    Page<Podcast> findByCreatorId(String creatorId, Pageable pageable);

    List<Podcast> findByCategory(String category);

    Page<Podcast> findByCategory(String category, Pageable pageable);

    List<Podcast> findByIsPublicTrue();

    Page<Podcast> findByIsPublicTrue(Pageable pageable);

    List<Podcast> findByCreatorIdAndIsPublicTrue(String creatorId);

    Page<Podcast> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    long countByCreatorId(String creatorId);
}
