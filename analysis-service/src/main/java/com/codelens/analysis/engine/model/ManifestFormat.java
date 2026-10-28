package com.codelens.analysis.engine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Descripcion declarativa de como extraer dependencias de un manifiesto.
 *
 * Igual que con los lenguajes y las reglas, soportar un gestor de paquetes nuevo
 * es anadir una entrada a {@code advisories/manifests.json}: el analizador de
 * dependencias no conoce ningun formato en concreto.
 *
 * @param ecosystem    etiqueta del ecosistema (npm, maven, pypi, go, cargo...)
 * @param filePattern  regex de ruta que identifica el manifiesto
 * @param entryPattern regex con los grupos nombrados {@code name} y {@code version}
 * @param description  para que sirve el formato, util en la documentacion
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManifestFormat(
        String ecosystem,
        String filePattern,
        String entryPattern,
        String description
) {
}
