package com.codelens.analysis.engine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Aviso de seguridad de una dependencia, cargado del catalogo offline
 * {@code advisories/advisories.json}.
 *
 * @param id               identificador del aviso (GHSA, CODELENS-xxx)
 * @param ecosystem        ecosistema del paquete (npm, maven, pypi, go...)
 * @param packageName      nombre del paquete tal y como aparece en el manifiesto
 * @param vulnerableRange  restriccion de versiones afectadas ("<2.17.1", ">=1.0 <1.4.2")
 * @param fixedVersion     primera version segura
 * @param severity         gravedad del aviso
 * @param cve              CVE asociado si existe
 * @param cwe              CWE asociado si existe
 * @param title            titulo corto
 * @param description      explicacion del impacto
 * @param remediation      accion concreta recomendada
 * @param references       enlaces
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Advisory(
        String id,
        String ecosystem,
        String packageName,
        String vulnerableRange,
        String fixedVersion,
        Severity severity,
        String cve,
        String cwe,
        String title,
        String description,
        String remediation,
        List<String> references
) {
    public Advisory {
        references = references == null ? List.of() : references;
        severity = severity == null ? Severity.HIGH : severity;
    }
}
