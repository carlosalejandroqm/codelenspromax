import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Severity } from '../core/models/analysis.model';
import { SEVERITY_META, SEVERITY_SINGULAR } from './severity';

/**
 * Badge de severidad de un hallazgo.
 *
 * Usa la paleta de estado y cumple su contrato: el color siempre viaja con la
 * palabra de severidad, de modo que quien no distinga los tonos lo lee igual. El
 * texto no se pinta con el color del dato, va con tinta legible sobre un fondo
 * tenido del mismo tono.
 */
@Component({
  selector: 'cl-severity-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="badge" [style.--badge-color]="color()">
      <span class="badge__dot" aria-hidden="true"></span>
      {{ text() }}
    </span>
  `,
  styleUrl: './severity-badge.component.scss',
})
export class SeverityBadgeComponent {
  readonly severity = input.required<Severity>();

  readonly color = computed(() => SEVERITY_META[this.severity()].statusToken);
  readonly text = computed(() => SEVERITY_SINGULAR[this.severity()]);
}
