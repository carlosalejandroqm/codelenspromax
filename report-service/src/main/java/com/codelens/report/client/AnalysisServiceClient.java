package com.codelens.report.client;

import com.codelens.report.config.ReportProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/** Cliente del microservicio de analisis. */
@Component
public class AnalysisServiceClient {

    private static final Duration TIMEOUT = Duration.ofMinutes(2);

    private final WebClient webClient;

    public AnalysisServiceClient(WebClient.Builder builder, ReportProperties properties) {
        this.webClient = builder.baseUrl(properties.getAnalysisServiceUrl()).build();
    }

    public AnalysisDetailView fetchDetail(String analysisId) {
        try {
            AnalysisDetailView detail = webClient.get()
                    .uri("/api/v1/analyses/{id}/detail", analysisId)
                    .retrieve()
                    .bodyToMono(AnalysisDetailView.class)
                    .block(TIMEOUT);
            if (detail == null || detail.analysis() == null) {
                throw new AnalysisUnavailableException("El analisis " + analysisId + " vino vacio");
            }
            return detail;
        } catch (WebClientResponseException.NotFound e) {
            throw new AnalysisUnavailableException("El analisis " + analysisId + " no existe");
        } catch (AnalysisUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AnalysisUnavailableException(
                    "No se pudo consultar el analysis-service: " + e.getMessage(), e);
        }
    }
}
