package org.podhub.podhub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeRequest {
    @NotBlank(message = "Episode ID is required")
    private String episodeId;
}
