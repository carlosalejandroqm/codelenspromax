package com.codelens.report;

import com.codelens.report.config.ReportProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Microservicio 3 - Reportes y recomendaciones.
 *
 * Toma el resultado del analisis y lo convierte en algo accionable: un resumen
 * ejecutivo, un plan de remediacion priorizado y el reporte exportable. No
 * vuelve a analizar codigo; su valor esta en priorizar y explicar.
 */
@SpringBootApplication
@EnableConfigurationProperties(ReportProperties.class)
public class ReportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportServiceApplication.class, args);
    }
}
