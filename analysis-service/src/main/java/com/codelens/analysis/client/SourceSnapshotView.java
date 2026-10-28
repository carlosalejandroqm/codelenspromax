package com.codelens.analysis.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Proyeccion del snapshot expuesto por el source-service. Es una copia local
 * deliberada: cada microservicio define su propio contrato de lectura y no
 * comparte clases con los demas.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SourceSnapshotView(
        String id,
        String type,
        String status,
        String projectName,
        String repositoryUrl,
        String branch,
        String commitId,
        long totalFiles,
        long analyzableFiles,
        long totalLines
) {
    public boolean isReady() {
        return "READY".equalsIgnoreCase(status);
    }
}
