package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Application error response")
public record AppErrorResponse(
        @Schema(description = "Human-readable error message", example = "User not found")
        String message,
        @Schema(description = "Error time as Unix timestamp in milliseconds", example = "1700000000000")
        long timestamp) {
}
