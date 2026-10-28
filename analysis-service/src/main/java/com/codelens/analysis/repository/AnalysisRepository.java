package com.codelens.analysis.repository;

import com.codelens.analysis.model.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, String> {

    List<Analysis> findAllByOrderByStartedAtDesc();

    List<Analysis> findBySourceIdOrderByStartedAtDesc(String sourceId);

    List<Analysis> findByOwnerIdOrderByStartedAtDesc(String ownerId);
}
