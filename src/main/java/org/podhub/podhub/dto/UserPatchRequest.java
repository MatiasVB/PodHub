package org.podhub.podhub.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.podhub.podhub.model.enums.UserStatus;

/**
 * DTO for partial user updates via PATCH.
 * All fields are nullable - only non-null fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPatchRequest {

    @Size(min = 2, max = 50, message = "Display name must be between 2 and 50 characters")
    private String displayName;

    @Pattern(regexp = "^https?://.*", message = "Avatar URL must be a valid HTTP/HTTPS URL")
    private String avatarUrl;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;

    // Admin-only field (enforced in service layer)
    private UserStatus status;
}
