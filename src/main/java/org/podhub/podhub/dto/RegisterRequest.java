package org.podhub.podhub.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 30)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$") String username,

        @NotBlank @Email @Size(max = 150) String email,

        @NotBlank @Size(min = 8, max = 256) String password,

        @Size(max = 150) String displayName,

        @Size(max = 2048) String avatarUrl
) {}