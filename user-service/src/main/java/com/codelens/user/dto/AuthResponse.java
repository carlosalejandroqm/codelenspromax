package com.codelens.user.dto;

import java.time.Instant;

/** Token Bearer que el frontend envia al gateway en cada peticion. */
public record AuthResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        UserDto user
) {
}
