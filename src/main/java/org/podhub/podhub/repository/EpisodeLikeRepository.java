package org.podhub.podhub.repository;

import org.podhub.podhub.model.EpisodeLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EpisodeLikeRepository extends MongoRepository<EpisodeLike, String> {

    boolean existsByUserIdAndEpisodeId(String userId, String episodeId);

    Optional<EpisodeLike> findByUserIdAndEpisodeId(String userId, String episodeId);

    Page<EpisodeLike> findByEpisodeId(String episodeId, Pageable pageable);

    Page<EpisodeLike> findByUserId(String userId, Pageable pageable);

    long countByEpisodeId(String episodeId);
}
