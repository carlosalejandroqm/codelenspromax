package com.codelens.analysis.engine.analyzers;

import com.codelens.analysis.config.AnalysisProperties;
import com.codelens.analysis.engine.AnalysisCollector;
import com.codelens.analysis.engine.model.AnalysisUnit;
import com.codelens.analysis.engine.model.FindingCategory;
import com.codelens.analysis.engine.model.RawFinding;
import com.codelens.analysis.engine.model.Severity;
import com.codelens.analysis.engine.spi.FileAnalyzer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detector de secretos por entropia de Shannon.
 *
 * Complementa a las reglas por patron: encuentra claves que no siguen ningun
 * formato conocido. Funciona igual en cualquier lenguaje porque solo mira
 * literales de cadena y su aleatoriedad, no la sintaxis que los rodea.
 */
@Component
public class EntropySecretAnalyzer implements FileAnalyzer {

    /** Cadenas entre comillas simples, dobles o backticks. */
    private static final Pattern LITERAL = Pattern.compile("[\"'`]([^\"'`\\n]{16,200})[\"'`]");

    /** Palabras en la misma linea que sugieren que el literal es una credencial. */
    private static final Pattern CREDENTIAL_HINT = Pattern.compile(
            "(?i)(secret|token|passwd|password|pwd|api[_-]?key|apikey|access[_-]?key|private[_-]?key|"
                    + "auth|credential|bearer|signature|salt|encrypt|client[_-]?secret|connection[_-]?string)");

    /** Literales que parecen datos legitimos y no claves. */
    private static final Pattern BENIGN = Pattern.compile(
            "(?i)^(https?://|[a-z]+://|\\./|\\.\\./|/[a-z/._-]+$|[a-z-]+/[a-z-]+$|"
                    + "\\{\\{|\\$\\{|%s|<[a-z/]|[0-9.]+$|lorem ipsum)");

    private final AnalysisProperties properties;

    public EntropySecretAnalyzer(AnalysisProperties properties) {
        this.properties = properties;
    }

    @Override
    public String id() {
        return "entropy-secret-scanner";
    }

    @Override
    public void analyze(AnalysisUnit unit, AnalysisCollector collector) {
        for (int i = 0; i < unit.codeLines().size(); i++) {
            String line = unit.codeLines().get(i);
            if (line.length() < properties.getSecretMinLength()) {
                continue;
            }
            boolean hinted = CREDENTIAL_HINT.matcher(line).find();
            Matcher matcher = LITERAL.matcher(line);
            while (matcher.find()) {
                String literal = matcher.group(1);
                if (!isSuspicious(literal, hinted)) {
                    continue;
                }
                int lineNumber = i + 1;
                double entropy = shannonEntropy(literal);
                collector.add(RawFinding.builder("SECRET-ENTROPY")
                        .title("Posible credencial embebida detectada por entropia")
                        .description(("Un literal de %d caracteres con entropia de Shannon %.2f bits/caracter "
                                + "aparece en el codigo%s. Los valores de alta entropia suelen ser claves, "
                                + "tokens o contrasenas que no deberian estar en el repositorio.")
                                .formatted(literal.length(), entropy,
                                        hinted ? " junto a un identificador de credencial" : ""))
                        .category(FindingCategory.SECRET)
                        .severity(hinted ? Severity.CRITICAL : Severity.MEDIUM)
                        .confidence(hinted ? 0.75 : 0.45)
                        .cwe("CWE-798")
                        .owasp("A07:2021-Identification and Authentication Failures")
                        .filePath(unit.path())
                        .lineNumber(lineNumber)
                        .evidence(mask(unit.evidenceAt(lineNumber), literal))
                        .language(unit.languageName())
                        .remediation("Mueve el valor a una variable de entorno o a un gestor de secretos "
                                + "(Vault, AWS Secrets Manager, Azure Key Vault), rota la credencial expuesta "
                                + "y purga el valor del historial de git.")
                        .build());
            }
        }
    }

    private boolean isSuspicious(String literal, boolean hinted) {
        if (literal.length() < properties.getSecretMinLength()) {
            return false;
        }
        if (BENIGN.matcher(literal).find()) {
            return false;
        }
        if (literal.chars().distinct().count() < 8) {
            return false;
        }
        if (literal.contains(" ")) {
            return false;
        }
        double entropy = shannonEntropy(literal);
        double threshold = hinted
                ? properties.getSecretEntropyThreshold() - 0.6
                : properties.getSecretEntropyThreshold();
        return entropy >= threshold;
    }

    /** Entropia de Shannon en bits por caracter. */
    static double shannonEntropy(String value) {
        Map<Character, Integer> frequency = new HashMap<>();
        for (char c : value.toCharArray()) {
            frequency.merge(c, 1, Integer::sum);
        }
        double entropy = 0.0;
        double length = value.length();
        for (int count : frequency.values()) {
            double probability = count / length;
            entropy -= probability * (Math.log(probability) / Math.log(2));
        }
        return entropy;
    }

    /** No se devuelve el secreto completo como evidencia. */
    private String mask(String evidence, String literal) {
        if (literal.length() <= 8) {
            return evidence;
        }
        String masked = literal.substring(0, 4) + "*".repeat(Math.min(12, literal.length() - 8))
                + literal.substring(literal.length() - 4);
        return evidence.replace(literal, masked);
    }
}
