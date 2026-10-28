package com.codelens.analysis.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Comparador de versiones y evaluador de restricciones suficiente para los
 * avisos de seguridad: soporta {@code <}, {@code <=}, {@code >}, {@code >=},
 * {@code =} y la conjuncion de varias restricciones separadas por espacios
 * ({@code ">=1.0.0 <1.4.2"}).
 *
 * No pretende ser semver completo: ignora metadatos de build y trata los
 * sufijos de pre-release como inferiores a la version final, que es el
 * comportamiento que necesitan los rangos de los avisos.
 */
public final class VersionRange {

    private final List<Constraint> constraints;

    private VersionRange(List<Constraint> constraints) {
        this.constraints = constraints;
    }

    public static VersionRange parse(String expression) {
        List<Constraint> parsed = new ArrayList<>();
        if (expression != null && !expression.isBlank()) {
            for (String token : expression.trim().split("[,\\s]+")) {
                Constraint constraint = Constraint.parse(token);
                if (constraint != null) {
                    parsed.add(constraint);
                }
            }
        }
        return new VersionRange(parsed);
    }

    /** true si la version cumple todas las restricciones del rango. */
    public boolean contains(String version) {
        String normalized = normalize(version);
        if (normalized.isEmpty() || constraints.isEmpty()) {
            return false;
        }
        for (Constraint constraint : constraints) {
            if (!constraint.test(normalized)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Limpia prefijos de rango que traen los manifiestos ({@code ^1.2.3},
     * {@code ~>1.0}, {@code v2.0}) y se queda con la version base.
     */
    public static String normalize(String version) {
        if (version == null) {
            return "";
        }
        String cleaned = version.trim()
                .replaceAll("^[\\^~>=<vV\\s]+", "")
                .replaceAll("[\"'\\s]", "");
        int space = cleaned.indexOf(' ');
        if (space > 0) {
            cleaned = cleaned.substring(0, space);
        }
        return cleaned.matches("^\\d.*") ? cleaned : "";
    }

    /** Compara dos versiones numero a numero; los pre-release van despues. */
    public static int compare(String left, String right) {
        String[] leftParts = splitNumeric(left);
        String[] rightParts = splitNumeric(right);
        int length = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < length; i++) {
            int a = i < leftParts.length ? toInt(leftParts[i]) : 0;
            int b = i < rightParts.length ? toInt(rightParts[i]) : 0;
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        boolean leftPre = left.matches(".*[-+].*");
        boolean rightPre = right.matches(".*[-+].*");
        if (leftPre == rightPre) {
            return 0;
        }
        return leftPre ? -1 : 1;
    }

    private static String[] splitNumeric(String version) {
        String base = version.split("[-+]")[0];
        return base.split("\\.");
    }

    private static int toInt(String part) {
        StringBuilder digits = new StringBuilder();
        for (char c : part.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            } else {
                break;
            }
        }
        return digits.isEmpty() ? 0 : Integer.parseInt(digits.toString());
    }

    /** Una sola restriccion, p.ej. {@code <2.17.1}. */
    private record Constraint(String operator, String version) {

        static Constraint parse(String token) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            for (String operator : List.of("<=", ">=", "<", ">", "==", "=")) {
                if (trimmed.startsWith(operator)) {
                    String version = normalize(trimmed.substring(operator.length()));
                    return version.isEmpty() ? null : new Constraint(operator, version);
                }
            }
            String version = normalize(trimmed);
            return version.isEmpty() ? null : new Constraint("=", version);
        }

        boolean test(String candidate) {
            int comparison = compare(candidate, version);
            return switch (operator) {
                case "<" -> comparison < 0;
                case "<=" -> comparison <= 0;
                case ">" -> comparison > 0;
                case ">=" -> comparison >= 0;
                default -> comparison == 0;
            };
        }
    }
}
