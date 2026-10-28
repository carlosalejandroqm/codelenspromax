package com.codelens.report.model;

/** Esfuerzo estimado de una recomendacion, para poder planificarla. */
public enum Effort {

    LOW("Bajo", "menos de 2 horas"),
    MEDIUM("Medio", "entre medio dia y 2 dias"),
    HIGH("Alto", "mas de 2 dias o requiere diseno");

    private final String label;
    private final String estimate;

    Effort(String label, String estimate) {
        this.label = label;
        this.estimate = estimate;
    }

    public String getLabel() { return label; }
    public String getEstimate() { return estimate; }
}
