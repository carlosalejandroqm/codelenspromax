package com.codelens.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto unico de entrada del sistema. Enruta el trafico del frontend Angular
 * hacia los tres microservicios de dominio y centraliza CORS.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
