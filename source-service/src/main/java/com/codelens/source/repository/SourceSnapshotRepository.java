package com.codelens.source.repository;

import com.codelens.source.model.SourceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SourceSnapshotRepository extends JpaRepository<SourceSnapshot, String> {

    List<SourceSnapshot> findAllByOrderByCreatedAtDesc();

    List<SourceSnapshot> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
}
