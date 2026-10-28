import { Verdict } from '../core/models/report.model';

/** Veredicto del reporte: color de estado siempre acompanado de su etiqueta. */
export const VERDICT_META: Record<Verdict, { label: string; token: string }> = {
  APROBADO: { label: 'Aprobado', token: 'var(--status-good)' },
  APROBADO_CON_RESERVAS: { label: 'Aprobado con reservas', token: 'var(--status-warning)' },
  RECHAZADO: { label: 'Rechazado', token: 'var(--status-critical)' },
};
