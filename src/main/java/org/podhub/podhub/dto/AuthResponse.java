package org.podhub.podhub.dto;

import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;
import java.util.List;

@Relation(collectionRelation = "auth")
public record AuthResponse(
        @JsonView(Views.Summary.class) String accessToken,
        @JsonView(Views.Summary.class) String refreshToken,
        @JsonView(Views.Summary.class) String tokenType,

        @JsonView(Views.Complete.class) long expiresIn,
        @JsonView(Views.Complete.class) long refreshExpiresIn,
        @JsonView(Views.Complete.class) Instant issuedAt,
        @JsonView(Views.Complete.class) Instant refreshIssuedAt,

        @JsonView(Views.Summary.class) UserResponse user,
        @JsonView(Views.Complete.class) List<String> authorities
) {

    public interface Views {
        interface Summary {}
        interface Complete extends Summary {}
    }

    public static AuthResponse of(
            String accessToken,
            String refreshToken,
            long expiresIn,
            long refreshExpiresIn,
            Instant issuedAt,
            Instant refreshIssuedAt,
            UserResponse user,
            List<String> authorities
    ) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                expiresIn,
                refreshExpiresIn,
                issuedAt,
                refreshIssuedAt,
                user,
                authorities
        );
    }
}
