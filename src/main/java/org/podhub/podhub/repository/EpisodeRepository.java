package org.podhub.podhub.repository;

import org.podhub.podhub.model.Episode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EpisodeRepository extends MongoRepository<Episode, String> {

    Page<Episode> findByPodcastId(String podcastId, Pageable pageable);

    Page<Episode> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Episode> findByIsPublicTrue(Pageable pageable);

    long countByPodcastId(String podcastId);
}
