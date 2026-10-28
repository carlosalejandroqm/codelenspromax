package com.codelens.source.model;

/** Origen desde el que se obtuvo el codigo. */
public enum SourceType {
    GIT,
    ZIP,
    /** Carpeta local seleccionada en el navegador y empaquetada como zip. */
    FOLDER
}
