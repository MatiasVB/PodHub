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
public class SubscriptionRequest {
    @NotBlank(message = "Podcast ID is required")
    private String podcastId;
}
