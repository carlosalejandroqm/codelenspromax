package com.codelens.source.repository;

import com.codelens.source.model.SourceFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SourceFileRepository extends JpaRepository<SourceFile, Long> {

    List<SourceFile> findBySnapshotIdOrderByPathAsc(String snapshotId);

    List<SourceFile> findBySnapshotIdAndAnalyzableTrueOrderByPathAsc(String snapshotId);

    Optional<SourceFile> findBySnapshotIdAndPath(String snapshotId, String path);
}
