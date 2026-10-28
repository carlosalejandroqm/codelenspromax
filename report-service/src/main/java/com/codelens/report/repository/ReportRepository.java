package com.codelens.report.repository;

import com.codelens.report.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, String> {

    List<Report> findAllByOrderByGeneratedAtDesc();

    List<Report> findByAnalysisIdOrderByGeneratedAtDesc(String analysisId);

    List<Report> findByOwnerIdOrderByGeneratedAtDesc(String ownerId);
}
