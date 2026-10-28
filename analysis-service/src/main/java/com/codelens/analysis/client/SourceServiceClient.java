package com.codelens.analysis.client;

import com.codelens.analysis.config.AnalysisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente del microservicio de codigo fuente.
 *
 * Descarga el contenido con concurrencia limitada: el analisis necesita todos
 * los archivos, pero abrir cientos de conexiones simultaneas solo castigaria al
 * servicio de origen.
 */
@Component
public class SourceServiceClient {

    private static final Logger log = LoggerFactory.getLogger(SourceServiceClient.class);
    private static final int FETCH_CONCURRENCY = 8;
    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    private final WebClient webClient;

    public SourceServiceClient(WebClient.Builder builder, AnalysisProperties properties) {
        this.webClient = builder.baseUrl(properties.getSourceServiceUrl()).build();
    }

    public SourceSnapshotView fetchSnapshot(String sourceId) {
        try {
            return webClient.get()
                    .uri("/api/v1/sources/{id}", sourceId)
                    .retrieve()
                    .bodyToMono(SourceSnapshotView.class)
                    .block(TIMEOUT);
        } catch (WebClientResponseException.NotFound e) {
            throw new SourceUnavailableException("El snapshot " + sourceId + " no existe en el source-service");
        } catch (RuntimeException e) {
            throw new SourceUnavailableException(
                    "No se pudo consultar el source-service: " + e.getMessage(), e);
        }
    }

    public List<SourceFileView> fetchAnalyzableFiles(String sourceId) {
        try {
            SourceFileView[] files = webClient.get()
                    .uri(uri -> uri.path("/api/v1/sources/{id}/files")
                            .queryParam("onlyAnalyzable", true)
                            .build(sourceId))
                    .retrieve()
                    .bodyToMono(SourceFileView[].class)
                    .block(TIMEOUT);
            return files == null ? List.of() : List.of(files);
        } catch (RuntimeException e) {
            throw new SourceUnavailableException(
                    "No se pudo obtener el inventario de archivos: " + e.getMessage(), e);
        }
    }

    /**
     * @return contenido indexado por ruta; los archivos que fallen se omiten y
     *         se registran, para que un archivo ilegible no anule el analisis.
     */
    public Map<String, String> fetchContents(String sourceId, List<SourceFileView> files) {
        Map<String, String> contents = Flux.fromIterable(files)
                .flatMap(file -> fetchContent(sourceId, file.path()), FETCH_CONCURRENCY)
                .collectMap(FileContentView::path, FileContentView::content, LinkedHashMap::new)
                .block(TIMEOUT);
        return contents == null ? Map.of() : contents;
    }

    private Mono<FileContentView> fetchContent(String sourceId, String path) {
        return webClient.get()
                .uri(uri -> uri.path("/api/v1/sources/{id}/content")
                        .queryParam("path", path)
                        .build(sourceId))
                .retrieve()
                .bodyToMono(FileContentView.class)
                .onErrorResume(error -> {
                    log.debug("Archivo omitido {}: {}", path, error.getMessage());
                    return Mono.empty();
                });
    }
}
