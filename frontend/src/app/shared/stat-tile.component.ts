import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Stat tile: una cifra con su etiqueta. Cuando el dato es un solo numero, el
 * numero es el grafico.
 */
@Component({
  selector: 'cl-stat-tile',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="tile">
      <span class="tile__label">{{ label() }}</span>
      <span class="tile__value">{{ value() }}<span class="tile__unit">{{ unit() }}</span></span>
      @if (note()) {
        <span class="tile__note">{{ note() }}</span>
      }
    </div>
  `,
  styleUrl: './stat-tile.component.scss',
})
export class StatTileComponent {
  readonly label = input.required<string>();
  readonly value = input.required<string | number>();
  readonly unit = input('');
  readonly note = input('');
}
