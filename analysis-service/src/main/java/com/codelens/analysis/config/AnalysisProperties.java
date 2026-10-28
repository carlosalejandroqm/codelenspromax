package com.codelens.analysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Parametros del motor de analisis, todos ajustables sin recompilar. */
@ConfigurationProperties(prefix = "codelens.analysis")
public class AnalysisProperties {

    /** URL base del microservicio de codigo fuente. */
    private String sourceServiceUrl = "http://localhost:8081";

    /** Directorio externo opcional con perfiles de lenguaje y reglas extra. */
    private String externalRulesDirectory = "";

    /** Numero maximo de archivos que se analizan por ejecucion. */
    private int maxFilesPerAnalysis = 5000;

    /** Hallazgos maximos por regla y archivo, para no inundar el reporte. */
    private int maxFindingsPerRulePerFile = 20;

    /** Lineas por encima de las cuales un archivo se considera demasiado largo. */
    private int longFileThreshold = 400;

    /** Complejidad ciclomatica a partir de la cual una funcion es un riesgo. */
    private int complexityThreshold = 15;

    /** Tamano de la ventana (en lineas) para detectar duplicacion. */
    private int duplicationWindowLines = 6;

    /** Entropia minima de Shannon para marcar un literal como posible secreto. */
    private double secretEntropyThreshold = 4.0;

    /** Longitud minima de un literal candidato a secreto. */
    private int secretMinLength = 20;

    public String getSourceServiceUrl() { return sourceServiceUrl; }
    public void setSourceServiceUrl(String sourceServiceUrl) { this.sourceServiceUrl = sourceServiceUrl; }
    public String getExternalRulesDirectory() { return externalRulesDirectory; }
    public void setExternalRulesDirectory(String externalRulesDirectory) { this.externalRulesDirectory = externalRulesDirectory; }
    public int getMaxFilesPerAnalysis() { return maxFilesPerAnalysis; }
    public void setMaxFilesPerAnalysis(int maxFilesPerAnalysis) { this.maxFilesPerAnalysis = maxFilesPerAnalysis; }
    public int getMaxFindingsPerRulePerFile() { return maxFindingsPerRulePerFile; }
    public void setMaxFindingsPerRulePerFile(int maxFindingsPerRulePerFile) { this.maxFindingsPerRulePerFile = maxFindingsPerRulePerFile; }
    public int getLongFileThreshold() { return longFileThreshold; }
    public void setLongFileThreshold(int longFileThreshold) { this.longFileThreshold = longFileThreshold; }
    public int getComplexityThreshold() { return complexityThreshold; }
    public void setComplexityThreshold(int complexityThreshold) { this.complexityThreshold = complexityThreshold; }
    public int getDuplicationWindowLines() { return duplicationWindowLines; }
    public void setDuplicationWindowLines(int duplicationWindowLines) { this.duplicationWindowLines = duplicationWindowLines; }
    public double getSecretEntropyThreshold() { return secretEntropyThreshold; }
    public void setSecretEntropyThreshold(double secretEntropyThreshold) { this.secretEntropyThreshold = secretEntropyThreshold; }
    public int getSecretMinLength() { return secretMinLength; }
    public void setSecretMinLength(int secretMinLength) { this.secretMinLength = secretMinLength; }
}
