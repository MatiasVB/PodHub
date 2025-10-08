package org.podhub.podhub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "listening_progress")
@CompoundIndexes({
    @CompoundIndex(name = "user_episode_unique", def = "{'userId': 1, 'episodeId': 1}", unique = true),
    @CompoundIndex(name = "user_updated", def = "{'userId': 1, 'updatedAt': -1}")
})
public class ListeningProgress {
    @Id
    private String id;

    private String userId;
    private String episodeId;
    private Integer positionSeconds;
    private Boolean completed;
    private Instant createdAt;
}
