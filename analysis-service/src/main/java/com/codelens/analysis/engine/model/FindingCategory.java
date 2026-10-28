package com.codelens.analysis.engine.model;

/** Familia a la que pertenece un hallazgo. */
public enum FindingCategory {

    /** Vulnerabilidad de seguridad en el codigo (SAST). */
    SECURITY,
    /** Credencial o secreto embebido en el repositorio. */
    SECRET,
    /** Dependencia con vulnerabilidad conocida (SCA). */
    DEPENDENCY,
    /** Problema de mantenibilidad: complejidad, tamano, duplicacion. */
    MAINTAINABILITY,
    /** Configuracion insegura o fragil. */
    CONFIGURATION,
    /** Deuda tecnica declarada por el propio equipo (TODO/FIXME/HACK). */
    TECHNICAL_DEBT
}
