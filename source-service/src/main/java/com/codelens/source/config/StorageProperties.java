package com.codelens.source.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Set;

/**
 * Limites y politica de ingesta. Todo configurable por properties para poder
 * ajustar el analisis sin recompilar.
 */
@ConfigurationProperties(prefix = "codelens.storage")
public class StorageProperties {

    /** Directorio raiz donde se materializan los snapshots. */
    private String workspaceRoot = System.getProperty("java.io.tmpdir") + "/codelens-workspaces";

    /** Tamano maximo de un archivo individual para entrar al analisis. */
    private long maxFileSizeBytes = 2L * 1024 * 1024;

    /** Tamano maximo del snapshot completo (proteccion anti zip-bomb). */
    private long maxSnapshotBytes = 300L * 1024 * 1024;

    /** Numero maximo de archivos inventariados. */
    private int maxFiles = 20_000;

    /** Directorios que nunca se analizan (dependencias, artefactos de build). */
    private Set<String> excludedDirectories = Set.of(
            ".git", ".svn", ".hg", "node_modules", "bower_components", "vendor",
            "target", "build", "dist", "out", "bin", "obj", ".next", ".nuxt",
            ".venv", "venv", "env", "__pycache__", ".mypy_cache", ".pytest_cache",
            ".gradle", ".idea", ".vscode", ".terraform", "coverage", "site-packages",
            ".dart_tool", "Pods", "DerivedData", ".angular", ".cache", "__MACOSX"
    );

    /** Extensiones binarias o de datos que se inventarian pero no se analizan. */
    private Set<String> binaryExtensions = Set.of(
            "png", "jpg", "jpeg", "gif", "bmp", "ico", "webp", "svgz", "tiff", "psd",
            "pdf", "zip", "gz", "tar", "rar", "7z", "bz2", "xz", "jar", "war", "ear",
            "class", "o", "so", "dylib", "dll", "exe", "bin", "dat", "db", "sqlite",
            "mp3", "mp4", "avi", "mov", "mkv", "wav", "flac", "ogg", "webm",
            "ttf", "otf", "woff", "woff2", "eot", "doc", "docx", "xls", "xlsx",
            "ppt", "pptx", "pyc", "pyo", "lock", "map", "min.js", "wasm", "node"
    );

    /** Hosts git permitidos; vacio = cualquiera. */
    private List<String> allowedGitHosts = List.of();

    public String getWorkspaceRoot() { return workspaceRoot; }
    public void setWorkspaceRoot(String workspaceRoot) { this.workspaceRoot = workspaceRoot; }
    public long getMaxFileSizeBytes() { return maxFileSizeBytes; }
    public void setMaxFileSizeBytes(long maxFileSizeBytes) { this.maxFileSizeBytes = maxFileSizeBytes; }
    public long getMaxSnapshotBytes() { return maxSnapshotBytes; }
    public void setMaxSnapshotBytes(long maxSnapshotBytes) { this.maxSnapshotBytes = maxSnapshotBytes; }
    public int getMaxFiles() { return maxFiles; }
    public void setMaxFiles(int maxFiles) { this.maxFiles = maxFiles; }
    public Set<String> getExcludedDirectories() { return excludedDirectories; }
    public void setExcludedDirectories(Set<String> excludedDirectories) { this.excludedDirectories = excludedDirectories; }
    public Set<String> getBinaryExtensions() { return binaryExtensions; }
    public void setBinaryExtensions(Set<String> binaryExtensions) { this.binaryExtensions = binaryExtensions; }
    public List<String> getAllowedGitHosts() { return allowedGitHosts; }
    public void setAllowedGitHosts(List<String> allowedGitHosts) { this.allowedGitHosts = allowedGitHosts; }
}
