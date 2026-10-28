package com.codelens.report.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracion del generador de reportes. */
@ConfigurationProperties(prefix = "codelens.report")
public class ReportProperties {

    /** URL base del microservicio de analisis. */
    private String analysisServiceUrl = "http://localhost:8082";

    /** Numero maximo de recomendaciones que se incluyen en el plan. */
    private int maxRecommendations = 25;

    /** Hallazgos de ejemplo que se citan por recomendacion. */
    private int examplesPerRecommendation = 3;

    /** Confianza minima para que un hallazgo influya en las recomendaciones. */
    private double minConfidence = 0.35;

    /** Directorio externo opcional con playbooks de remediacion adicionales. */
    private String externalPlaybooksDirectory = "";

    public String getAnalysisServiceUrl() { return analysisServiceUrl; }
    public void setAnalysisServiceUrl(String analysisServiceUrl) { this.analysisServiceUrl = analysisServiceUrl; }
    public int getMaxRecommendations() { return maxRecommendations; }
    public void setMaxRecommendations(int maxRecommendations) { this.maxRecommendations = maxRecommendations; }
    public int getExamplesPerRecommendation() { return examplesPerRecommendation; }
    public void setExamplesPerRecommendation(int v) { this.examplesPerRecommendation = v; }
    public double getMinConfidence() { return minConfidence; }
    public void setMinConfidence(double minConfidence) { this.minConfidence = minConfidence; }
    public String getExternalPlaybooksDirectory() { return externalPlaybooksDirectory; }
    public void setExternalPlaybooksDirectory(String v) { this.externalPlaybooksDirectory = v; }
}
