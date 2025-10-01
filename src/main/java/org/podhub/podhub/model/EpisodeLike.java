package org.podhub.podhub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "episode_likes")
@CompoundIndexes({
    @CompoundIndex(name = "user_episode_unique", def = "{'userId': 1, 'episodeId': 1}", unique = true),
    @CompoundIndex(name = "episode_idx", def = "{'episodeId': 1}")
})
public class EpisodeLike {
    @Id
    private String id;

    private String userId;
    private String episodeId;
    private Instant createdAt;
}
