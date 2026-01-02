package com.docusign.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserEmailContext(
        String userId,
        String email,
        String userName,
        @JsonProperty("isExternal")
        boolean isExternal
) {}
