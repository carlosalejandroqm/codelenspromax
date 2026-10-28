export type Severity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';

export type FindingCategory =
  | 'SECURITY'
  | 'SECRET'
  | 'DEPENDENCY'
  | 'MAINTAINABILITY'
  | 'CONFIGURATION'
  | 'TECHNICAL_DEBT';

export type AnalysisStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';

/** Reparto de un lenguaje en el proyecto analizado. */
export interface LanguageStat {
  language: string;
  files: number;
  codeLines: number;
  percentage: number;
}

/** Resumen agregado de un analisis. */
export interface Analysis {
  id: string;
  sourceId: string;
  projectName: string;
  status: AnalysisStatus;
  startedAt: string;
  completedAt: string | null;
  durationMillis: number;
  filesAnalyzed: number;
  totalLines: number;
  codeLines: number;
  commentLines: number;
  blankLines: number;
  securityScore: number;
  healthScore: number;
  overallScore: number;
  grade: string;
  riskLevel: Severity;
  criticalCount: number;
  highCount: number;
  mediumCount: number;
  lowCount: number;
  infoCount: number;
  securityFindings: number;
  secretFindings: number;
  dependencyFindings: number;
  maintainabilityFindings: number;
  technicalDebtFindings: number;
  duplicationRatio: number;
  avgComplexity: number;
  maxComplexity: number;
  testFileRatio: number;
  todoCount: number;
  declaredDependencies: number;
  subScores: Record<string, number>;
  languages: LanguageStat[];
  errorMessage: string | null;
}

/** Hallazgo individual detectado por el motor. */
export interface Finding {
  id: number;
  ruleId: string;
  title: string;
  description: string;
  category: FindingCategory;
  severity: Severity;
  confidence: number;
  cwe: string | null;
  owasp: string | null;
  filePath: string | null;
  lineNumber: number;
  evidence: string | null;
  language: string | null;
  remediation: string | null;
}

export interface AnalysisDetail {
  analysis: Analysis;
  findings: Finding[];
}

/** Cobertura del motor: demuestra que las reglas son configurables. */
export interface EngineInfo {
  activeAnalyzers: string[];
  languageProfiles: number;
  activeRules: number;
  advisories: number;
  manifestFormats: number;
  ruleLoadErrors: string[];
}
