package com.codelens.source.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Copia inmutable del codigo obtenido en un momento dado. Es la unidad que el
 * resto de microservicios referencia (sourceId) para analizar.
 */
@Entity
@Table(name = "source_snapshot")
public class SourceSnapshot {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SourceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SnapshotStatus status = SnapshotStatus.PENDING;

    /** Nombre legible del proyecto (repo o nombre del zip). */
    @Column(nullable = false, length = 255)
    private String projectName;

    /** URL del repositorio remoto, null cuando el origen es un zip. */
    @Column(length = 1024)
    private String repositoryUrl;

    @Column(length = 255)
    private String branch;

    /** Commit resuelto tras el clonado, null para zips. */
    @Column(length = 64)
    private String commitId;

    @Column(length = 1024)
    private String workspacePath;

    /** Usuario que importo el codigo (X-User-Id del gateway); null en llamadas directas. */
    @Column(length = 36)
    private String ownerId;

    private long totalFiles;
    private long analyzableFiles;
    private long skippedFiles;
    private long totalBytes;
    private long totalLines;

    @Column(length = 2000)
    private String errorMessage;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant completedAt;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SourceFile> files = new ArrayList<>();

    protected SourceSnapshot() {
    }

    public SourceSnapshot(String id, SourceType type, String projectName) {
        this.id = id;
        this.type = type;
        this.projectName = projectName;
    }

    public void markReady(Instant when) {
        this.status = SnapshotStatus.READY;
        this.completedAt = when;
    }

    public void markFailed(String message) {
        this.status = SnapshotStatus.FAILED;
        this.errorMessage = message != null && message.length() > 1990 ? message.substring(0, 1990) : message;
        this.completedAt = Instant.now();
    }

    public void addFile(SourceFile file) {
        file.setSnapshot(this);
        this.files.add(file);
    }

    public String getId() { return id; }
    public SourceType getType() { return type; }
    public SnapshotStatus getStatus() { return status; }
    public void setStatus(SnapshotStatus status) { this.status = status; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getCommitId() { return commitId; }
    public void setCommitId(String commitId) { this.commitId = commitId; }
    public String getWorkspacePath() { return workspacePath; }
    public void setWorkspacePath(String workspacePath) { this.workspacePath = workspacePath; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public long getTotalFiles() { return totalFiles; }
    public void setTotalFiles(long totalFiles) { this.totalFiles = totalFiles; }
    public long getAnalyzableFiles() { return analyzableFiles; }
    public void setAnalyzableFiles(long analyzableFiles) { this.analyzableFiles = analyzableFiles; }
    public long getSkippedFiles() { return skippedFiles; }
    public void setSkippedFiles(long skippedFiles) { this.skippedFiles = skippedFiles; }
    public long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }
    public long getTotalLines() { return totalLines; }
    public void setTotalLines(long totalLines) { this.totalLines = totalLines; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public List<SourceFile> getFiles() { return files; }
}
