package com.codelens.analysis.engine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Regla declarativa de deteccion. El motor no conoce ninguna regla en codigo:
 * todas se cargan de {@code resources/rules/*.json} y, opcionalmente, de un
 * directorio externo, de forma que el catalogo es extensible en caliente.
 *
 * @param id           identificador estable, se usa para mapear recomendaciones
 * @param name         titulo corto del problema
 * @param description  que ocurre y por que es un riesgo
 * @param category     familia del hallazgo
 * @param severity     gravedad por defecto
 * @param confidence   0..1, cuanta confianza tiene la deteccion por regex
 * @param cwe          identificador CWE ("CWE-89")
 * @param owasp        categoria OWASP Top 10 ("A03:2021-Injection")
 * @param languages    ids de lenguaje a los que aplica; ["*"] = todos
 * @param pattern      regex que dispara el hallazgo (sobre el codigo sin comentarios)
 * @param notPattern   regex que, si coincide en la misma linea, descarta el hallazgo
 * @param pathInclude  regex de ruta que debe coincidir para evaluar la regla
 * @param pathExclude  regex de ruta que excluye la evaluacion
 * @param scanComments true para evaluar tambien comentarios (TODO/FIXME)
 * @param remediation  como corregirlo
 * @param references   enlaces de apoyo
 * @param enabled      permite desactivar una regla sin borrarla
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RuleDefinition(
        String id,
        String name,
        String description,
        FindingCategory category,
        Severity severity,
        Double confidence,
        String cwe,
        String owasp,
        List<String> languages,
        String pattern,
        String notPattern,
        String pathInclude,
        String pathExclude,
        Boolean scanComments,
        String remediation,
        List<String> references,
        Boolean enabled
) {

    public static final String ANY_LANGUAGE = "*";

    public RuleDefinition {
        languages = languages == null || languages.isEmpty() ? List.of(ANY_LANGUAGE) : languages;
        references = references == null ? List.of() : references;
        confidence = confidence == null ? 0.7 : confidence;
        category = category == null ? FindingCategory.SECURITY : category;
        severity = severity == null ? Severity.MEDIUM : severity;
        scanComments = scanComments != null && scanComments;
        enabled = enabled == null || enabled;
    }

    public boolean appliesToAllLanguages() {
        return languages.contains(ANY_LANGUAGE);
    }
}
