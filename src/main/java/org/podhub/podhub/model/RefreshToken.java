package org.podhub.podhub.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "refresh_tokens")
@CompoundIndex(name = "idx_user_revoked", def = "{'userId': 1, 'revoked': 1}")
public class RefreshToken {

    @Id
    private String id; // UUID

    @Indexed(unique = true)
    private String token; // opaco/aleatorio

    @Indexed
    private String userId; // referencia al usuario (sin DBRef)

    private Instant expiresAt;

    private boolean revoked;

    private String replacedBy; // para rotación

    @CreatedDate
    private Instant createdAt;
}
