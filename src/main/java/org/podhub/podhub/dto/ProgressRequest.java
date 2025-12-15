package org.podhub.podhub.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressRequest {
    @NotNull(message = "Position is required")
    @Min(value = 0, message = "Position must be non-negative")
    private Integer positionSeconds;

    @NotNull(message = "Completed flag is required")
    private Boolean completed;
}
