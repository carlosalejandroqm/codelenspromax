package com.codelens.report.config;

import java.time.Instant;

/** Cuerpo de error uniforme del microservicio de reportes. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path);
    }
}
