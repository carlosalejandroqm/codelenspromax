import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { scoreStatusLabel, scoreStatusToken } from './severity';

/**
 * Meter: un ratio contra un limite (puntaje sobre 100).
 *
 * El relleno lleva el estado y el track no usado es un paso mas claro del mismo
 * color, de modo que el estado se lee a lo largo de toda la barra. El valor va
 * siempre como etiqueta visible, asi que el color nunca comunica solo.
 */
@Component({
  selector: 'cl-meter',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="meter">
      <div class="meter__head">
        <span class="meter__label">{{ label() }}</span>
        <span class="meter__value">
          {{ value().toFixed(precision()) }}<span class="meter__unit">{{ unit() }}</span>
        </span>
      </div>
      <div
        class="meter__track"
        role="meter"
        [attr.aria-label]="label()"
        [attr.aria-valuenow]="value()"
        aria-valuemin="0"
        [attr.aria-valuemax]="max()"
        [style.--meter-fill]="fillToken()"
      >
        <div class="meter__fill" [style.width.%]="percentage()"></div>
      </div>
      @if (showStatus()) {
        <span class="meter__status">{{ statusLabel() }}</span>
      }
    </div>
  `,
  styleUrl: './meter.component.scss',
})
export class MeterComponent {
  readonly label = input.required<string>();
  readonly value = input.required<number>();
  readonly max = input(100);
  readonly unit = input('');
  readonly precision = input(1);
  readonly showStatus = input(false);

  /** Token fijo opcional: si no se pasa, el color sale de la banda del puntaje. */
  readonly fill = input<string | null>(null);

  readonly percentage = computed(() => {
    const max = this.max() || 1;
    return Math.max(0, Math.min(100, (this.value() / max) * 100));
  });

  readonly fillToken = computed(
    () => this.fill() ?? scoreStatusToken((this.value() / (this.max() || 1)) * 100),
  );

  readonly statusLabel = computed(() =>
    scoreStatusLabel((this.value() / (this.max() || 1)) * 100),
  );
}
