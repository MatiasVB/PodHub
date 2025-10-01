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
@Document(collection = "subscriptions")
@CompoundIndexes({
    @CompoundIndex(name = "user_podcast_unique", def = "{'userId': 1, 'podcastId': 1}", unique = true),
    @CompoundIndex(name = "user_created", def = "{'userId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "podcast_idx", def = "{'podcastId': 1}")
})
public class Subscription {
    @Id
    private String id;

    private String userId;
    private String podcastId;
    private Boolean notifications;
    private Instant createdAt;
}
