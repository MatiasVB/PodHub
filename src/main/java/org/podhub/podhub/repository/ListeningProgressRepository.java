package org.podhub.podhub.repository;

import org.podhub.podhub.model.ListeningProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ListeningProgressRepository extends MongoRepository<ListeningProgress, String> {

    Optional<ListeningProgress> findByUserIdAndEpisodeId(String userId, String episodeId);

    Page<ListeningProgress> findByUserId(String userId, Pageable pageable);

    Page<ListeningProgress> findByEpisodeId(String episodeId, Pageable pageable);

    long countByEpisodeId(String episodeId);
}
