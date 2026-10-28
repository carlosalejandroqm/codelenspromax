package com.codelens.source.dto;

import com.codelens.source.model.SourceFile;

/** Entrada del inventario expuesta a los demas microservicios. */
public record SourceFileDto(
        String path,
        String extension,
        String language,
        long sizeBytes,
        int lineCount,
        boolean analyzable,
        String skipReason,
        String sha256
) {
    public static SourceFileDto from(SourceFile f) {
        return new SourceFileDto(f.getPath(), f.getExtension(), f.getLanguage(), f.getSizeBytes(),
                f.getLineCount(), f.isAnalyzable(), f.getSkipReason(), f.getSha256());
    }
}
