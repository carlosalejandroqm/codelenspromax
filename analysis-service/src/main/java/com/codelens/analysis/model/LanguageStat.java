package com.codelens.analysis.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Reparto de un lenguaje dentro del analisis. */
@Embeddable
public class LanguageStat {

    @Column(name = "language", length = 64)
    private String language;

    @Column(name = "files")
    private int files;

    @Column(name = "code_lines")
    private long codeLines;

    @Column(name = "percentage")
    private double percentage;

    protected LanguageStat() {
    }

    public LanguageStat(String language, int files, long codeLines, double percentage) {
        this.language = language;
        this.files = files;
        this.codeLines = codeLines;
        this.percentage = percentage;
    }

    public String getLanguage() { return language; }
    public int getFiles() { return files; }
    public long getCodeLines() { return codeLines; }
    public double getPercentage() { return percentage; }
}
