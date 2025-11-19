package org.podhub.podhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Para login (email o username + password)
 */
public record AuthRequest(
        @NotBlank @Size(max = 150) String identifier,
        @NotBlank @Size(min = 8, max = 256) String password,
        Boolean rememberMe
) {}
