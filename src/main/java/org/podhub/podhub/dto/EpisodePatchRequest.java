package org.podhub.podhub.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for partial episode updates via PATCH.
 * All fields are nullable - only non-null fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpisodePatchRequest {

    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @Min(value = 0, message = "Season must be non-negative")
    private Integer season;

    @Min(value = 0, message = "Episode number must be non-negative")
    private Integer number;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    @Size(max = 50000, message = "Transcript cannot exceed 50000 characters")
    private String transcript;

    private Boolean explicit;

    private Boolean isPublic;

    private Instant publishAt;
}
