package org.podhub.podhub.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.podhub.podhub.model.enums.UserRole;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "roles")
public class Role {

    @Id
    private String id; // UUID

    @Indexed(unique = true)
    private UserRole name; // USER, CREATOR, ADMIN

    // Evitamos DBRef: guardamos nombres de permiso
    @Builder.Default
    private Set<String> permissionNames = new HashSet<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Role addPermission(String permissionName) {
        this.permissionNames.add(permissionName);
        return this;
    }
}
