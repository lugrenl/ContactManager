package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Validation error response")
public record ValidationErrorResponse(
        @Schema(description = "Summary message", example = "Validation failed")
        String message,
        @Schema(description = "Error time as Unix timestamp in milliseconds", example = "1700000000000")
        long timestamp,
        @Schema(description = "List of field validation errors", example = "[\"name: Name is required\"]")
        List<String> details) {
}
