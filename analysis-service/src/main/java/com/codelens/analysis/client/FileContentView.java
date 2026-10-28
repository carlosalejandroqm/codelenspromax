package com.codelens.analysis.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Contenido de un archivo devuelto por el source-service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FileContentView(
        String sourceId,
        String path,
        String language,
        int lineCount,
        String content
) {
}
