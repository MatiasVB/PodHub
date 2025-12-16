package org.podhub.podhub.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.podhub.podhub.model.enums.UserStatus;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id; // genera UUID en el servicio al crear

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private String displayName;
    private String avatarUrl;
    private String bio;

    // Referencias a Role por id (sin DBRef)
    @Builder.Default
    private Set<String> roleIds = new HashSet<>();

    private UserStatus status;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
