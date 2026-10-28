package com.codelens.analysis.engine;

import com.codelens.analysis.engine.model.AnalysisUnit;
import com.codelens.analysis.engine.model.LanguageProfile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Convierte texto plano en un {@link AnalysisUnit}, separando codigo,
 * estructura y comentarios con un automata de estados guiado unicamente por el
 * {@link LanguageProfile}. Al no haber gramatica por lenguaje, el mismo
 * algoritmo sirve para Java, Python, Go, Rust o un lenguaje que no conozcamos.
 */
@Component
public class SourceNormalizer {

    private static final Pattern LINE_SPLIT = Pattern.compile("\r\n|\r|\n");

    public AnalysisUnit normalize(String path, String content, LanguageProfile profile) {
        List<String> raw = List.of(LINE_SPLIT.split(content, -1));
        Scan scan = scan(raw, profile);
        boolean isTest = matchesTestPath(path, profile);
        return new AnalysisUnit(path, profile, raw, scan.code, scan.structure, scan.comments, isTest);
    }

    private Scan scan(List<String> raw, LanguageProfile profile) {
        List<String> code = new ArrayList<>(raw.size());
        List<String> structure = new ArrayList<>(raw.size());
        List<String> comments = new ArrayList<>(raw.size());

        // Marcador de cierre pendiente cuando un comentario de bloque abarca
        // varias lineas; persiste entre iteraciones a proposito.
        String closeBlock = null;

        for (String line : raw) {
            StringBuilder codeLine = new StringBuilder();
            StringBuilder structureLine = new StringBuilder();
            StringBuilder commentLine = new StringBuilder();

            int i = 0;
            String activeString = null;

            while (i < line.length()) {
                // 1. Dentro de un comentario de bloque: buscar su cierre.
                if (closeBlock != null) {
                    int end = line.indexOf(closeBlock, i);
                    if (end < 0) {
                        commentLine.append(line, i, line.length());
                        i = line.length();
                    } else {
                        commentLine.append(line, i, end);
                        i = end + closeBlock.length();
                        closeBlock = null;
                    }
                    continue;
                }

                // 2. Dentro de un literal: copiar tal cual hasta el cierre.
                if (activeString != null) {
                    if (isEscapeSequence(line, i)) {
                        codeLine.append(line, i, i + 2);
                        structureLine.append("  ");
                        i += 2;
                        continue;
                    }
                    if (line.startsWith(activeString, i)) {
                        codeLine.append(activeString);
                        structureLine.append(' ');
                        i += activeString.length();
                        activeString = null;
                        continue;
                    }
                    codeLine.append(line.charAt(i));
                    structureLine.append(' ');
                    i++;
                    continue;
                }

                // 3. Comentario de linea: el resto de la linea es comentario.
                String lineMarker = matchAt(line, i, profile.lineComments());
                if (lineMarker != null) {
                    commentLine.append(line, i + lineMarker.length(), line.length());
                    break;
                }

                // 4. Apertura de comentario de bloque.
                LanguageProfile.BlockComment block = matchBlockStart(line, i, profile);
                if (block != null) {
                    closeBlock = block.end();
                    i += block.start().length();
                    continue;
                }

                // 5. Apertura de literal de cadena.
                String delimiter = matchAt(line, i, profile.stringDelimiters());
                if (delimiter != null) {
                    activeString = delimiter;
                    codeLine.append(delimiter);
                    structureLine.append(' ');
                    i += delimiter.length();
                    continue;
                }

                // 6. Codigo normal.
                codeLine.append(line.charAt(i));
                structureLine.append(line.charAt(i));
                i++;
            }

            code.add(codeLine.toString());
            structure.add(structureLine.toString());
            comments.add(commentLine.toString());
        }

        return new Scan(code, structure, comments);
    }

    /**
     * Secuencia de escape dentro de un literal. Se consume el par completo para
     * que un {@code \\"} no se confunda con el cierre de la cadena.
     */
    private boolean isEscapeSequence(String line, int index) {
        return line.charAt(index) == '\\' && index + 1 < line.length();
    }

    private String matchAt(String line, int index, List<String> markers) {
        for (String marker : markers) {
            if (!marker.isEmpty() && line.startsWith(marker, index)) {
                return marker;
            }
        }
        return null;
    }

    private LanguageProfile.BlockComment matchBlockStart(String line, int index, LanguageProfile profile) {
        for (LanguageProfile.BlockComment block : profile.blockComments()) {
            if (block.start() != null && !block.start().isEmpty() && line.startsWith(block.start(), index)) {
                return block;
            }
        }
        return null;
    }

    private boolean matchesTestPath(String path, LanguageProfile profile) {
        String normalized = path.toLowerCase();
        for (String regex : profile.testPathPatterns()) {
            if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(normalized).find()) {
                return true;
            }
        }
        return normalized.contains("/test/") || normalized.contains("/tests/")
                || normalized.startsWith("test/") || normalized.startsWith("tests/");
    }

    private record Scan(List<String> code, List<String> structure, List<String> comments) {
    }
}
