export type Priority = 'P0' | 'P1' | 'P2' | 'P3';

export type Effort = 'LOW' | 'MEDIUM' | 'HIGH';

export type Verdict = 'APROBADO' | 'APROBADO_CON_RESERVAS' | 'RECHAZADO';

/** Accion concreta recomendada al equipo. */
export interface Recommendation {
  rank: number;
  priority: Priority;
  priorityLabel: string;
  timeWindow: string;
  effort: Effort;
  effortEstimate: string;
  title: string;
  problem: string;
  action: string;
  rationale: string;
  category: string | null;
  cwe: string | null;
  owasp: string | null;
  ownerRole: string | null;
  findingsCount: number;
  affectedFiles: number;
  examples: string[];
  ruleIds: string[];
  references: string[];
}

/** Reporte generado a partir de un analisis. */
export interface Report {
  id: string;
  analysisId: string;
  sourceId: string | null;
  projectName: string;
  generatedAt: string;
  verdict: Verdict;
  riskLevel: string | null;
  grade: string;
  overallScore: number;
  securityScore: number;
  healthScore: number;
  executiveSummary: string;
  strengths: string[];
  keyRisks: string[];
  recommendations: Recommendation[];
}

/** Fila de la lista de reportes. */
export interface ReportSummary {
  id: string;
  analysisId: string;
  projectName: string;
  generatedAt: string;
  verdict: Verdict;
  grade: string;
  overallScore: number;
  recommendations: number;
}
