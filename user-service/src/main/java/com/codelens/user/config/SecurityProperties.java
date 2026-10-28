package com.codelens.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Secreto compartido con el gateway y vigencia de los tokens emitidos. */
@ConfigurationProperties(prefix = "codelens.security")
public class SecurityProperties {

    /** Clave HMAC-SHA256; debe ser la misma que usa el gateway para validar. */
    private String jwtSecret;

    private Duration tokenTtl = Duration.ofHours(8);

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
    public Duration getTokenTtl() { return tokenTtl; }
    public void setTokenTtl(Duration tokenTtl) { this.tokenTtl = tokenTtl; }
}
