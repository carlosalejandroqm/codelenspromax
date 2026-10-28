package com.codelens.analysis.dto;

import java.util.List;

/**
 * Descripcion del motor: sirve para demostrar que el analizador es extensible y
 * que su cobertura depende de configuracion, no de codigo.
 *
 * @param activeAnalyzers  analizadores enchufados al pipeline
 * @param languageProfiles perfiles de lenguaje cargados
 * @param activeRules      reglas declarativas activas
 * @param advisories       avisos de dependencias en el catalogo offline
 * @param manifestFormats  formatos de manifiesto reconocidos
 * @param ruleLoadErrors   reglas descartadas y su motivo
 */
public record EngineInfoDto(
        List<String> activeAnalyzers,
        int languageProfiles,
        int activeRules,
        int advisories,
        int manifestFormats,
        List<String> ruleLoadErrors
) {
}
