package com.codelens.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Autenticacion centralizada: el gateway es el unico que conoce el token.
 *
 * Valida el Bearer emitido por el user-service y reenvia la identidad a los
 * microservicios en X-User-Id / X-User-Name. Esas cabeceras se borran siempre
 * de la peticion original para que un cliente no pueda suplantar a otro usuario.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_NAME_HEADER = "X-User-Name";

    /** Rutas que no requieren token. */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/**",
            "/actuator/**");

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final JwtParser parser;

    public JwtAuthenticationFilter(@Value("${codelens.security.jwt-secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("codelens.security.jwt-secret debe tener al menos 32 bytes");
        }
        this.parser = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(bytes)).build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        ServerHttpRequest.Builder sanitized = request.mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_NAME_HEADER);
                });

        if (HttpMethod.OPTIONS.equals(request.getMethod()) || isPublic(path)) {
            return chain.filter(exchange.mutate().request(sanitized.build()).build());
        }

        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(exchange, "Debes iniciar sesion para usar la API");
        }

        Claims claims;
        try {
            claims = parser.parseSignedClaims(header.substring(7).trim()).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(exchange, "El token no es valido o ya expiro");
        }

        String username = claims.get("username", String.class);
        ServerHttpRequest authenticated = sanitized
                .header(USER_ID_HEADER, claims.getSubject())
                .header(USER_NAME_HEADER, username == null ? "" : username)
                .build();
        return chain.filter(exchange.mutate().request(authenticated).build());
    }

    /** Antes del enrutamiento para no abrir conexiones a servicios si el token falla. */
    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.stream().anyMatch(pattern -> matcher.match(pattern, path));
    }

    /** Mismo formato de error (ApiError) que devuelven los microservicios. */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\",\"path\":\"%s\"}"
                .formatted(Instant.now(), message, exchange.getRequest().getPath().value().replace("\"", ""));
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
