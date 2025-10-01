package org.podhub.podhub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "episodes")
@CompoundIndex(name = "podcast_publish", def = "{'podcastId': 1, 'publishAt': -1}")
public class Episode {
    @Id
    private String id;

    private String podcastId;
    private String title;
    private Integer season;
    private Integer number;
    private String description;
    private String audioUrl;
    private Integer durationSec;
    private Boolean explicit;
    private Instant publishAt;
    private String transcript;
    private Instant createdAt;
    private Instant updatedAt;
}
