package com.codelens.report.model;

/**
 * Prioridad de una recomendacion, con la ventana de tiempo sugerida.
 * Es lo que convierte una lista de hallazgos en un plan de trabajo.
 */
public enum Priority {

    P0("Inmediato", "Antes del siguiente despliegue"),
    P1("Alta", "En el sprint actual"),
    P2("Media", "En el proximo sprint"),
    P3("Baja", "Cuando haya capacidad");

    private final String label;
    private final String window;

    Priority(String label, String window) {
        this.label = label;
        this.window = window;
    }

    public String getLabel() { return label; }
    public String getWindow() { return window; }
}
