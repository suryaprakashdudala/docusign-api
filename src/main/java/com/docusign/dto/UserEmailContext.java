package com.docusign.dto;

public record UserEmailContext(
        String userId,
        String email,
        String userName,
        boolean isExternal
) {}
