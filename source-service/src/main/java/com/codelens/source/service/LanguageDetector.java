package com.codelens.source.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Etiquetado best-effort del lenguaje por extension o nombre de archivo.
 *
 * Es deliberadamente simple: el analisis real usa el registro de perfiles del
 * analysis-service, que es extensible por configuracion. Aqui solo se necesita
 * una etiqueta para el inventario y las estadisticas del snapshot.
 */
@Component
public class LanguageDetector {

    private static final Map<String, String> BY_EXTENSION = Map.ofEntries(
            Map.entry("java", "Java"), Map.entry("kt", "Kotlin"), Map.entry("kts", "Kotlin"),
            Map.entry("scala", "Scala"), Map.entry("groovy", "Groovy"), Map.entry("clj", "Clojure"),
            Map.entry("js", "JavaScript"), Map.entry("jsx", "JavaScript"), Map.entry("mjs", "JavaScript"),
            Map.entry("cjs", "JavaScript"), Map.entry("ts", "TypeScript"), Map.entry("tsx", "TypeScript"),
            Map.entry("py", "Python"), Map.entry("pyi", "Python"), Map.entry("rb", "Ruby"),
            Map.entry("php", "PHP"), Map.entry("go", "Go"), Map.entry("rs", "Rust"),
            Map.entry("c", "C"), Map.entry("h", "C"), Map.entry("cpp", "C++"), Map.entry("cc", "C++"),
            Map.entry("cxx", "C++"), Map.entry("hpp", "C++"), Map.entry("hh", "C++"),
            Map.entry("cs", "C#"), Map.entry("swift", "Swift"), Map.entry("m", "Objective-C"),
            Map.entry("dart", "Dart"), Map.entry("lua", "Lua"), Map.entry("pl", "Perl"),
            Map.entry("r", "R"), Map.entry("jl", "Julia"), Map.entry("ex", "Elixir"),
            Map.entry("exs", "Elixir"), Map.entry("erl", "Erlang"), Map.entry("hs", "Haskell"),
            Map.entry("sh", "Shell"), Map.entry("bash", "Shell"), Map.entry("zsh", "Shell"),
            Map.entry("ps1", "PowerShell"), Map.entry("bat", "Batch"), Map.entry("cmd", "Batch"),
            Map.entry("sql", "SQL"), Map.entry("html", "HTML"), Map.entry("htm", "HTML"),
            Map.entry("vue", "Vue"), Map.entry("svelte", "Svelte"), Map.entry("css", "CSS"),
            Map.entry("scss", "SCSS"), Map.entry("sass", "Sass"), Map.entry("less", "Less"),
            Map.entry("json", "JSON"), Map.entry("yaml", "YAML"), Map.entry("yml", "YAML"),
            Map.entry("xml", "XML"), Map.entry("toml", "TOML"), Map.entry("ini", "INI"),
            Map.entry("properties", "Properties"), Map.entry("md", "Markdown"),
            Map.entry("tf", "Terraform"), Map.entry("dockerfile", "Dockerfile"),
            Map.entry("gradle", "Gradle"), Map.entry("cfg", "Config"), Map.entry("conf", "Config"),
            Map.entry("env", "DotEnv"), Map.entry("txt", "Text"), Map.entry("svg", "SVG")
    );

    private static final Map<String, String> BY_FILENAME = Map.ofEntries(
            Map.entry("dockerfile", "Dockerfile"),
            Map.entry("makefile", "Makefile"),
            Map.entry("jenkinsfile", "Groovy"),
            Map.entry("gemfile", "Ruby"),
            Map.entry("rakefile", "Ruby"),
            Map.entry("procfile", "Config"),
            Map.entry("cmakelists.txt", "CMake"),
            Map.entry(".gitignore", "Config"),
            Map.entry(".env", "DotEnv")
    );

    /** @return etiqueta de lenguaje, u "Other" si no se reconoce la extension. */
    public String detect(String fileName, String extension) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        String byName = BY_FILENAME.get(lowerName);
        if (byName != null) {
            return byName;
        }
        if (lowerName.startsWith(".env")) {
            return "DotEnv";
        }
        if (extension == null || extension.isBlank()) {
            return "Other";
        }
        return BY_EXTENSION.getOrDefault(extension.toLowerCase(Locale.ROOT), "Other");
    }

    /** Extrae la extension sin punto, o cadena vacia si no tiene. */
    public String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
