package com.zak.jobhunter.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Standard error response envelope")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        @Schema(description = "HTTP status code") int status,
        @Schema(description = "Short error code") String error,
        @Schema(description = "Human-readable message") String message,
        @Schema(description = "Request path") String path,
        @Schema(description = "Timestamp") Instant timestamp,
        @Schema(description = "Field-level validation errors") List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, Instant.now(), null);
    }

    public static ApiError withFieldErrors(int status, String error, String message, String path,
                                           List<FieldError> fieldErrors) {
        return new ApiError(status, error, message, path, Instant.now(), fieldErrors);
    }
}
