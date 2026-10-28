package com.codelens.analysis.engine.model;

import java.util.List;

/**
 * Un archivo preparado para el analisis, con tres vistas paralelas linea a linea
 * (todas del mismo tamano que {@code rawLines}, indice 0 = linea 1):
 *
 * <ul>
 *   <li>{@code codeLines}: sin comentarios, con los literales intactos. Es la
 *       vista que usan las reglas de vulnerabilidad, porque muchos secretos y
 *       consultas SQL viven dentro de cadenas.</li>
 *   <li>{@code structureLines}: sin comentarios y con el contenido de los
 *       literales borrado. Es la vista para metricas estructurales, donde una
 *       llave dentro de una cadena no debe contar.</li>
 *   <li>{@code commentLines}: solo el texto de los comentarios, para medir
 *       documentacion y deuda tecnica declarada.</li>
 * </ul>
 *
 * Nada de esto depende del lenguaje concreto: las tres vistas se derivan del
 * {@link LanguageProfile}, incluido el perfil generico.
 */
public record AnalysisUnit(
        String path,
        LanguageProfile profile,
        List<String> rawLines,
        List<String> codeLines,
        List<String> structureLines,
        List<String> commentLines,
        boolean testFile
) {

    public int lineCount() {
        return rawLines.size();
    }

    /** Linea original (1-based) recortada, para adjuntar como evidencia. */
    public String evidenceAt(int lineNumber) {
        if (lineNumber < 1 || lineNumber > rawLines.size()) {
            return "";
        }
        String line = rawLines.get(lineNumber - 1).trim();
        return line.length() > 240 ? line.substring(0, 240) + "..." : line;
    }

    public String languageName() {
        return profile.name();
    }
}
