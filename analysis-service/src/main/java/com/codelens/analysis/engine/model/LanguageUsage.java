package com.codelens.analysis.engine.model;

/**
 * Peso de un lenguaje en el proyecto analizado.
 *
 * @param language   nombre del lenguaje
 * @param files      archivos analizados de ese lenguaje
 * @param codeLines  lineas de codigo efectivas
 * @param percentage porcentaje sobre el total de lineas de codigo
 */
public record LanguageUsage(
        String language,
        int files,
        long codeLines,
        double percentage
) {
}
