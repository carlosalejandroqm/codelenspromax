package com.codelens.analysis.engine.model;

/** Gravedad de un hallazgo, con el peso que aporta al score de seguridad. */
public enum Severity {

    CRITICAL(10, 25.0),
    HIGH(8, 12.0),
    MEDIUM(5, 5.0),
    LOW(3, 1.5),
    INFO(1, 0.3);

    private final int rank;
    private final double penalty;

    Severity(int rank, double penalty) {
        this.rank = rank;
        this.penalty = penalty;
    }

    public int getRank() { return rank; }

    /** Penalizacion base que resta al score de seguridad. */
    public double getPenalty() { return penalty; }
}
