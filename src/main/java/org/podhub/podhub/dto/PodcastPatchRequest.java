package org.podhub.podhub.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for partial podcast updates via PATCH.
 * All fields are nullable - only non-null fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodcastPatchRequest {

    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
             message = "Slug must contain only lowercase letters, numbers, and hyphens")
    private String slug;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @Size(max = 10, message = "Language code cannot exceed 10 characters")
    private String language;

    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category;

    @Pattern(regexp = "^https?://.*", message = "Cover image URL must be a valid HTTP/HTTPS URL")
    private String coverImageUrl;

    private Boolean isPublic;
}
