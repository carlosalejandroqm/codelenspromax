package com.codelens.report.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Accion concreta recomendada al equipo. Agrupa todos los hallazgos que
 * comparten causa para que el plan tenga tantas entradas como problemas reales,
 * no tantas como lineas afectadas.
 */
@Entity
@Table(name = "recommendation", indexes = @Index(name = "idx_reco_report", columnList = "report_id"))
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    /** Posicion en el plan; 1 es lo primero que hay que hacer. */
    private int rank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Effort effort;

    @Column(nullable = false, length = 255)
    private String title;

    /** Que esta mal, en lenguaje del equipo. */
    @Column(length = 1500)
    private String problem;

    /** Que hay que hacer, en imperativo y verificable. */
    @Column(length = 1500)
    private String action;

    /** Por que importa: impacto si no se hace. */
    @Column(length = 1500)
    private String rationale;

    @Column(length = 32)
    private String category;

    @Column(length = 32)
    private String cwe;

    @Column(length = 96)
    private String owasp;

    @Column(length = 64)
    private String ownerRole;

    private int findingsCount;
    private int affectedFiles;

    /** Ubicaciones de ejemplo, para que el equipo sepa donde empezar. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recommendation_example", joinColumns = @JoinColumn(name = "recommendation_id"))
    @Column(name = "location", length = 1024)
    private List<String> examples = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recommendation_rule", joinColumns = @JoinColumn(name = "recommendation_id"))
    @Column(name = "rule_id", length = 96)
    private List<String> ruleIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recommendation_reference", joinColumns = @JoinColumn(name = "recommendation_id"))
    @Column(name = "url", length = 512)
    private List<String> references = new ArrayList<>();

    protected Recommendation() {
    }

    public Recommendation(Priority priority, Effort effort, String title, String problem,
                          String action, String rationale, String category) {
        this.priority = priority;
        this.effort = effort;
        this.title = title;
        this.problem = problem;
        this.action = action;
        this.rationale = rationale;
        this.category = category;
    }

    void setReport(Report report) { this.report = report; }
    public void setRank(int rank) { this.rank = rank; }

    public Long getId() { return id; }
    public int getRank() { return rank; }
    public Priority getPriority() { return priority; }
    public Effort getEffort() { return effort; }
    public String getTitle() { return title; }
    public String getProblem() { return problem; }
    public String getAction() { return action; }
    public String getRationale() { return rationale; }
    public String getCategory() { return category; }
    public String getCwe() { return cwe; }
    public void setCwe(String cwe) { this.cwe = cwe; }
    public String getOwasp() { return owasp; }
    public void setOwasp(String owasp) { this.owasp = owasp; }
    public String getOwnerRole() { return ownerRole; }
    public void setOwnerRole(String ownerRole) { this.ownerRole = ownerRole; }
    public int getFindingsCount() { return findingsCount; }
    public void setFindingsCount(int findingsCount) { this.findingsCount = findingsCount; }
    public int getAffectedFiles() { return affectedFiles; }
    public void setAffectedFiles(int affectedFiles) { this.affectedFiles = affectedFiles; }
    public List<String> getExamples() { return examples; }
    public List<String> getRuleIds() { return ruleIds; }
    public List<String> getReferences() { return references; }
}
