package com.codelens.analysis.repository;

import com.codelens.analysis.engine.model.FindingCategory;
import com.codelens.analysis.engine.model.Severity;
import com.codelens.analysis.model.Finding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FindingRepository extends JpaRepository<Finding, Long> {

    List<Finding> findByAnalysisIdOrderBySeverityAscConfidenceDesc(String analysisId);

    List<Finding> findByAnalysisIdAndSeverity(String analysisId, Severity severity);

    List<Finding> findByAnalysisIdAndCategory(String analysisId, FindingCategory category);
}
