package com.codelens.analysis;

import com.codelens.analysis.config.AnalysisProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Microservicio 2 - Analisis de datos del codigo.
 *
 * El motor es agnostico del lenguaje: no hay ningun {@code if (language ==
 * JAVA)} en la logica. Todo comportamiento especifico de un lenguaje vive en
 * {@code resources/languages/languages.json} (perfiles) y todas las reglas de
 * vulnerabilidad en {@code resources/rules/*.json}. Un lenguaje desconocido cae
 * en el perfil generico y aun asi se analiza.
 */
@SpringBootApplication
@EnableConfigurationProperties(AnalysisProperties.class)
public class AnalysisServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalysisServiceApplication.class, args);
    }
}
