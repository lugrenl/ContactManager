package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing a JWT token")
public record JwtResponse(
        @Schema(description = "Signed JWT token to send in the Authorization header",
                example = "eyJhbGciOiJIUzI1NiJ9...")
        @JsonProperty("jwt-token")
        String jwtToken) {
}
