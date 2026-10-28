package com.codelens.source;

import com.codelens.source.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Microservicio 1 - Obtencion de codigo fuente.
 *
 * Responsabilidad unica: materializar un "snapshot" inmutable de codigo en el
 * workspace local, ya sea clonando un repositorio remoto o descomprimiendo un
 * zip, e inventariar sus archivos de texto para que otros servicios los lean.
 */
@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class SourceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SourceServiceApplication.class, args);
    }
}
