package com.codelens.report.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Guia de remediacion declarativa asociada a una o varias reglas.
 *
 * Igual que el catalogo de reglas del analizador, los playbooks viven en JSON:
 * mejorar el consejo que recibe el usuario no requiere recompilar el servicio.
 *
 * @param ruleMatch  regex sobre el id de la regla (p.ej. "^SQLI-" o "^DEP-")
 * @param title      titulo de la recomendacion
 * @param action     que hacer, en imperativo
 * @param rationale  por que importa
 * @param effort     LOW, MEDIUM o HIGH
 * @param ownerRole  perfil del equipo que deberia tomarlo
 * @param priority   prioridad forzada; si falta se deduce de la gravedad
 * @param references enlaces de apoyo
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Playbook(
        String ruleMatch,
        String title,
        String action,
        String rationale,
        String effort,
        String ownerRole,
        String priority,
        List<String> references
) {
    public Playbook {
        references = references == null ? List.of() : references;
    }
}
