package org.podhub.podhub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "podcasts")
@CompoundIndex(name = "creator_created", def = "{'creatorId': 1, 'createdAt': -1}")
public class Podcast {
    @Id
    private String id;

    private String creatorId;

    private String title;

    @Indexed(unique = true)
    private String slug;

    private String description;
    private String language;
    private String category;
    private String coverImageUrl;
    private Boolean isPublic;
    private Instant createdAt;
    private Instant updatedAt;
}
