package com.codelens.analysis.dto;

import com.codelens.analysis.model.LanguageStat;

/** Reparto de lenguajes del proyecto. */
public record LanguageStatDto(
        String language,
        int files,
        long codeLines,
        double percentage
) {
    public static LanguageStatDto from(LanguageStat stat) {
        return new LanguageStatDto(stat.getLanguage(), stat.getFiles(),
                stat.getCodeLines(), stat.getPercentage());
    }
}
