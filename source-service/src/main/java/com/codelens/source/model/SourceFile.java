package com.codelens.source.model;

import jakarta.persistence.*;

/**
 * Entrada del inventario de archivos de un snapshot. El contenido NO se guarda
 * en base de datos: se lee del workspace bajo demanda para no inflar la BD.
 */
@Entity
@Table(name = "source_file", indexes = @Index(name = "idx_file_snapshot", columnList = "snapshot_id"))
public class SourceFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private SourceSnapshot snapshot;

    /** Ruta relativa a la raiz del proyecto, siempre con separador '/'. */
    @Column(nullable = false, length = 1024)
    private String path;

    @Column(length = 32)
    private String extension;

    /** Etiqueta de lenguaje best-effort; el analizador la resuelve de nuevo. */
    @Column(length = 64)
    private String language;

    private long sizeBytes;
    private int lineCount;

    /** true cuando el archivo es texto y entra al analisis. */
    private boolean analyzable;

    @Column(length = 64)
    private String skipReason;

    @Column(length = 64)
    private String sha256;

    protected SourceFile() {
    }

    public SourceFile(String path, String extension, String language, long sizeBytes,
                      int lineCount, boolean analyzable, String skipReason, String sha256) {
        this.path = path;
        this.extension = extension;
        this.language = language;
        this.sizeBytes = sizeBytes;
        this.lineCount = lineCount;
        this.analyzable = analyzable;
        this.skipReason = skipReason;
        this.sha256 = sha256;
    }

    public Long getId() { return id; }
    public SourceSnapshot getSnapshot() { return snapshot; }
    public void setSnapshot(SourceSnapshot snapshot) { this.snapshot = snapshot; }
    public String getPath() { return path; }
    public String getExtension() { return extension; }
    public String getLanguage() { return language; }
    public long getSizeBytes() { return sizeBytes; }
    public int getLineCount() { return lineCount; }
    public boolean isAnalyzable() { return analyzable; }
    public String getSkipReason() { return skipReason; }
    public String getSha256() { return sha256; }
}
