package com.codelens.source.dto;

import com.codelens.source.model.SnapshotStatus;
import com.codelens.source.model.SourceSnapshot;
import com.codelens.source.model.SourceType;

import java.time.Instant;

/** Vista publica de un snapshot. */
public record SourceSnapshotDto(
        String id,
        SourceType type,
        SnapshotStatus status,
        String projectName,
        String repositoryUrl,
        String branch,
        String commitId,
        long totalFiles,
        long analyzableFiles,
        long skippedFiles,
        long totalBytes,
        long totalLines,
        String errorMessage,
        Instant createdAt,
        Instant completedAt
) {
    public static SourceSnapshotDto from(SourceSnapshot s) {
        return new SourceSnapshotDto(
                s.getId(), s.getType(), s.getStatus(), s.getProjectName(), s.getRepositoryUrl(),
                s.getBranch(), s.getCommitId(), s.getTotalFiles(), s.getAnalyzableFiles(),
                s.getSkippedFiles(), s.getTotalBytes(), s.getTotalLines(), s.getErrorMessage(),
                s.getCreatedAt(), s.getCompletedAt());
    }
}
