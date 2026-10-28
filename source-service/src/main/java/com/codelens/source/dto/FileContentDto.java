package com.codelens.source.dto;

/** Contenido de un archivo del snapshot, leido del workspace bajo demanda. */
public record FileContentDto(
        String sourceId,
        String path,
        String language,
        int lineCount,
        String content
) {
}
