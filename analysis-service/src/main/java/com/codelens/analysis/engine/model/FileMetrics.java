package com.codelens.analysis.engine.model;

/**
 * Metricas de salud calculadas para un archivo. Todas se derivan del perfil de
 * lenguaje, por lo que se obtienen igual para cualquier lenguaje.
 *
 * @param path              archivo medido
 * @param language          lenguaje resuelto
 * @param totalLines        lineas fisicas
 * @param codeLines         lineas con codigo efectivo
 * @param commentLines      lineas de comentario
 * @param blankLines        lineas en blanco
 * @param functionCount     funciones/metodos detectados
 * @param maxComplexity     complejidad ciclomatica de la funcion mas compleja
 * @param avgComplexity     complejidad media por funcion
 * @param maxNestingDepth   profundidad maxima de anidamiento
 * @param longestFunction   lineas de la funcion mas larga
 * @param todoCount         marcas de deuda tecnica en comentarios
 * @param testFile          si el archivo es de pruebas
 */
public record FileMetrics(
        String path,
        String language,
        int totalLines,
        int codeLines,
        int commentLines,
        int blankLines,
        int functionCount,
        int maxComplexity,
        double avgComplexity,
        int maxNestingDepth,
        int longestFunction,
        int todoCount,
        boolean testFile
) {
}
