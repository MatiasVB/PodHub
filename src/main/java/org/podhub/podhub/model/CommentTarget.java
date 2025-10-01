package org.podhub.podhub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.podhub.podhub.model.enums.CommentTargetType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentTarget {
    private CommentTargetType type;
    private String id;
}
