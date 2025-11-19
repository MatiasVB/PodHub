package org.podhub.podhub.dto;

import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.hateoas.server.core.Relation;
import org.podhub.podhub.model.User;

import java.time.Instant;
import java.util.List;

@Relation(collectionRelation = "users")
public record UserResponse(
        @JsonView(Views.Summary.class) String id,
        @JsonView(Views.Summary.class) String username,
        @JsonView(Views.Summary.class) String email,
        @JsonView(Views.Summary.class) String displayName,
        @JsonView(Views.Summary.class) String avatarUrl,
        @JsonView(Views.Summary.class) List<String> roles,

        @JsonView(Views.Complete.class) Instant createdAt,
        @JsonView(Views.Complete.class) Instant updatedAt
) {

    public interface Views {
        interface Summary {}
        interface Complete extends Summary {}
    }

    public static UserResponse from(User user, List<String> roles) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                roles,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
