package com.codelens.analysis.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Entrada del inventario de archivos que devuelve el source-service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SourceFileView(
        String path,
        String extension,
        String language,
        long sizeBytes,
        int lineCount,
        boolean analyzable
) {
}
