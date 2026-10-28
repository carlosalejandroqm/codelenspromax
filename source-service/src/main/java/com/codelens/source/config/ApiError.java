package com.codelens.source.config;

import java.time.Instant;

/** Cuerpo de error uniforme para todos los endpoints. */
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
