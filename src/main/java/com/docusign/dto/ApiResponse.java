package com.docusign.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private boolean success;
    private String message;
    private Instant timestamp;

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = Instant.now();
    }
}
