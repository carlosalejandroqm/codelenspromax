import { Severity } from '../core/models/analysis.model';

/**
 * Mapa de severidad a token de color y etiqueta.
 *
 * Dos escalas distintas, cada una donde corresponde:
 *
 * - Las **barras** de distribucion miden magnitud (cuantos hallazgos hay por
 *   nivel), asi que usan un ramp secuencial de un solo tono, mas oscuro cuanto
 *   mas grave. Es la opcion segura y esta validada como ramp ordinal en los dos
 *   modos.
 * - Los **badges** de un hallazgo concreto usan la paleta de estado, que es fija
 *   y nunca se tematiza. Por contrato esa paleta siempre va acompanada de la
 *   etiqueta de severidad, nunca comunica por color solo.
 */
export interface SeverityMeta {
  severity: Severity;
  label: string;
  /** Token del ramp ordinal, para las barras de magnitud. */
  rampToken: string;
  /** Token de la paleta de estado, para los badges. */
  statusToken: string;
  /** Peso para ordenar de mas grave a menos grave. */
  rank: number;
}

export const SEVERITY_ORDER: Severity[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'];

export const SEVERITY_META: Record<Severity, SeverityMeta> = {
  CRITICAL: { severity: 'CRITICAL', label: 'Criticos', rampToken: 'var(--sev-critical)', statusToken: 'var(--status-critical)', rank: 5 },
  HIGH: { severity: 'HIGH', label: 'Altos', rampToken: 'var(--sev-high)', statusToken: 'var(--status-serious)', rank: 4 },
  MEDIUM: { severity: 'MEDIUM', label: 'Medios', rampToken: 'var(--sev-medium)', statusToken: 'var(--status-warning)', rank: 3 },
  LOW: { severity: 'LOW', label: 'Bajos', rampToken: 'var(--sev-low)', statusToken: 'var(--accent)', rank: 2 },
  INFO: { severity: 'INFO', label: 'Informativos', rampToken: 'var(--sev-info)', statusToken: 'var(--text-muted)', rank: 1 },
};

/** Etiqueta en singular, para un hallazgo individual. */
export const SEVERITY_SINGULAR: Record<Severity, string> = {
  CRITICAL: 'Critico',
  HIGH: 'Alto',
  MEDIUM: 'Medio',
  LOW: 'Bajo',
  INFO: 'Informativo',
};

export const CATEGORY_LABEL: Record<string, string> = {
  SECURITY: 'Seguridad',
  SECRET: 'Secretos',
  DEPENDENCY: 'Dependencias',
  CONFIGURATION: 'Configuracion',
  MAINTAINABILITY: 'Mantenibilidad',
  TECHNICAL_DEBT: 'Deuda tecnica',
};

/**
 * Banda de estado de un puntaje 0-100. El relleno de los meters toma su color
 * de aqui, de modo que el estado se lee en toda la barra.
 */
export function scoreStatusToken(score: number): string {
  if (score >= 80) return 'var(--status-good)';
  if (score >= 60) return 'var(--status-warning)';
  if (score >= 40) return 'var(--status-serious)';
  return 'var(--status-critical)';
}

export function scoreStatusLabel(score: number): string {
  if (score >= 80) return 'Bueno';
  if (score >= 60) return 'Aceptable';
  if (score >= 40) return 'Deficiente';
  return 'Critico';
}

/** Tokens categoricos en orden fijo; el excedente cae en "Otros". */
export const CATEGORICAL_TOKENS = [
  'var(--cat-1)',
  'var(--cat-2)',
  'var(--cat-3)',
  'var(--cat-4)',
  'var(--cat-5)',
  'var(--cat-6)',
];

export const CATEGORICAL_OTHER = 'var(--cat-other)';
