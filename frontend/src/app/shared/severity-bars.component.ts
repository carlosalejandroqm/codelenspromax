import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Severity } from '../core/models/analysis.model';
import { SEVERITY_META, SEVERITY_ORDER } from './severity';

interface SeverityRow {
  severity: Severity;
  label: string;
  count: number;
  color: string;
  widthPercent: number;
}

/**
 * Distribucion de hallazgos por severidad.
 *
 * Es un grafico de magnitud, asi que usa el ramp secuencial de un solo tono
 * (mas oscuro = mas grave) en lugar de colores de estado. Cada barra lleva su
 * etiqueta y su valor directamente, de modo que el color solo refuerza el orden
 * y nunca es el unico canal. Sin leyenda: hay una sola serie y las barras estan
 * etiquetadas.
 */
@Component({
  selector: 'cl-severity-bars',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="bars">
      @for (row of rows(); track row.severity) {
        <div class="bars__row" [title]="row.count + ' hallazgos de severidad ' + row.label.toLowerCase()">
          <span class="bars__label">{{ row.label }}</span>
          <div class="bars__track">
            @if (row.count > 0) {
              <div class="bars__bar" [style.width.%]="row.widthPercent" [style.background]="row.color"></div>
            }
          </div>
          <span class="bars__value">{{ row.count }}</span>
        </div>
      }
      @if (total() === 0) {
        <p class="bars__empty">Sin hallazgos registrados.</p>
      }
    </div>
  `,
  styleUrl: './severity-bars.component.scss',
})
export class SeverityBarsComponent {
  readonly counts = input.required<Record<Severity, number>>();

  readonly total = computed(() =>
    SEVERITY_ORDER.reduce((sum, severity) => sum + (this.counts()[severity] ?? 0), 0),
  );

  readonly rows = computed<SeverityRow[]>(() => {
    const counts = this.counts();
    const peak = Math.max(1, ...SEVERITY_ORDER.map((severity) => counts[severity] ?? 0));
    return SEVERITY_ORDER.map((severity) => {
      const meta = SEVERITY_META[severity];
      const count = counts[severity] ?? 0;
      return {
        severity,
        label: meta.label,
        count,
        color: meta.rampToken,
        widthPercent: (count / peak) * 100,
      };
    });
  });
}
