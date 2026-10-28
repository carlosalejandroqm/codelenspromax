package com.codelens.analysis.engine;

import com.codelens.analysis.engine.model.LanguageProfile;
import com.codelens.analysis.engine.model.RuleDefinition;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Regla con sus expresiones ya compiladas y su ambito resuelto. Se construye
 * una sola vez al arrancar para no recompilar regex por cada linea de codigo.
 */
public final class CompiledRule {

    private final RuleDefinition definition;
    private final Pattern pattern;
    private final Pattern notPattern;
    private final Pattern pathInclude;
    private final Pattern pathExclude;
    private final Set<String> languageIds;

    private CompiledRule(RuleDefinition definition, Pattern pattern, Pattern notPattern,
                         Pattern pathInclude, Pattern pathExclude, Set<String> languageIds) {
        this.definition = definition;
        this.pattern = pattern;
        this.notPattern = notPattern;
        this.pathInclude = pathInclude;
        this.pathExclude = pathExclude;
        this.languageIds = languageIds;
    }

    /**
     * @throws java.util.regex.PatternSyntaxException si la regla trae un regex invalido
     */
    public static CompiledRule of(RuleDefinition definition) {
        int flags = Pattern.CASE_INSENSITIVE;
        return new CompiledRule(
                definition,
                Pattern.compile(definition.pattern(), flags),
                compileOrNull(definition.notPattern(), flags),
                compileOrNull(definition.pathInclude(), flags),
                compileOrNull(definition.pathExclude(), flags),
                definition.appliesToAllLanguages() ? Set.of() : Set.copyOf(definition.languages()));
    }

    private static Pattern compileOrNull(String regex, int flags) {
        return regex == null || regex.isBlank() ? null : Pattern.compile(regex, flags);
    }

    /** Decide si la regla debe evaluarse sobre este archivo. */
    public boolean appliesTo(String path, LanguageProfile profile) {
        if (!languageIds.isEmpty() && !languageIds.contains(profile.id())) {
            return false;
        }
        if (pathInclude != null && !pathInclude.matcher(path).find()) {
            return false;
        }
        return pathExclude == null || !pathExclude.matcher(path).find();
    }

    /** true si la linea dispara la regla y no esta exenta por {@code notPattern}. */
    public boolean matches(String line) {
        if (!pattern.matcher(line).find()) {
            return false;
        }
        return notPattern == null || !notPattern.matcher(line).find();
    }

    public RuleDefinition definition() { return definition; }
    public String id() { return definition.id(); }
    public boolean scansComments() { return definition.scanComments(); }
}
