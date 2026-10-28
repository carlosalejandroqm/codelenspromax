package com.codelens.analysis.engine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Set;

/**
 * Descripcion declarativa de un lenguaje. Es la unica pieza que "sabe" de
 * sintaxis, y se carga desde JSON: anadir soporte para un lenguaje nuevo es
 * anadir un objeto a {@code languages.json}, sin tocar codigo Java.
 *
 * @param id                 identificador estable (p.ej. "python")
 * @param name               nombre legible ("Python")
 * @param extensions         extensiones sin punto que mapean a este lenguaje
 * @param fileNames          nombres exactos de archivo (Dockerfile, Makefile...)
 * @param lineComments       marcadores de comentario de linea ("//", "#", "--")
 * @param blockComments      pares inicio/fin de comentario de bloque
 * @param stringDelimiters   delimitadores de literales de cadena
 * @param blockStyle         BRACES o INDENT: como se delimitan los bloques
 * @param branchKeywords     palabras que suman complejidad ciclomatica
 * @param functionPatterns   regex que marcan el inicio de una funcion/metodo
 * @param testPathPatterns   regex de ruta que identifican archivos de prueba
 * @param manifestFiles      manifiestos de dependencias del ecosistema
 * @param generic            true en el perfil de reserva para lenguajes desconocidos
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LanguageProfile(
        String id,
        String name,
        Set<String> extensions,
        Set<String> fileNames,
        List<String> lineComments,
        List<BlockComment> blockComments,
        List<String> stringDelimiters,
        BlockStyle blockStyle,
        Set<String> branchKeywords,
        List<String> functionPatterns,
        List<String> testPathPatterns,
        Set<String> manifestFiles,
        boolean generic
) {

    /** Par de marcadores de comentario multilinea. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BlockComment(String start, String end) {
    }

    /** Forma en que el lenguaje delimita bloques anidados. */
    public enum BlockStyle {
        BRACES,
        INDENT
    }

    public LanguageProfile {
        extensions = extensions == null ? Set.of() : extensions;
        fileNames = fileNames == null ? Set.of() : fileNames;
        lineComments = lineComments == null ? List.of() : lineComments;
        blockComments = blockComments == null ? List.of() : blockComments;
        stringDelimiters = stringDelimiters == null ? List.of("\"", "'") : stringDelimiters;
        blockStyle = blockStyle == null ? BlockStyle.BRACES : blockStyle;
        branchKeywords = branchKeywords == null ? Set.of() : branchKeywords;
        functionPatterns = functionPatterns == null ? List.of() : functionPatterns;
        testPathPatterns = testPathPatterns == null ? List.of() : testPathPatterns;
        manifestFiles = manifestFiles == null ? Set.of() : manifestFiles;
    }
}
