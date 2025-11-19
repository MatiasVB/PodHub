package org.podhub.podhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Para solicitar nuevos tokens a partir del refresh token.
 */
public record RefreshRequest(
        @NotBlank @Size(min = 20, max = 1024) String refreshToken
) {}