package org.podhub.podhub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.podhub.podhub.model.enums.CommentStatus;
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
@Document(collection = "comments")
@CompoundIndexes({
    @CompoundIndex(name = "target_created", def = "{'target.id': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "parent_idx", def = "{'parentId': 1}")
})
public class Comment {
    @Id
    private String id;

    private String userId;
    private CommentTarget target;
    private String content;
    private String parentId;
    private CommentStatus status;
    private Instant createdAt;
    private Instant editedAt;
}
