package org.example.util;

import java.util.List;

public record ValidationErrorResponse(String message, long timestamp, List<String> details) {
}
