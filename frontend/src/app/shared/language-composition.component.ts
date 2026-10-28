import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LanguageStat } from '../core/models/analysis.model';
import { CATEGORICAL_OTHER, CATEGORICAL_TOKENS } from './severity';

interface Segment {
  language: string;
  percentage: number;
  files: number;
  codeLines: number;
  color: string;
}

/**
 * Composicion del proyecto por lenguaje: es una relacion parte-todo, asi que
 * una barra apilada horizontal.
 *
 * Los tonos categoricos se asignan en orden fijo y nunca se ciclan: a partir del
 * septimo lenguaje todo se agrupa en "Otros" en lugar de generar colores nuevos.
 * La leyenda siempre esta presente con el nombre y el porcentaje visibles, que es
 * ademas el relieve que exigen los tonos claros de la paleta sobre fondo claro.
 */
@Component({
  selector: 'cl-language-composition',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (segments().length === 0) {
      <p class="empty-text">Sin lenguajes detectados.</p>
    } @else {
      <div class="stack" role="img" [attr.aria-label]="ariaLabel()">
        @for (segment of segments(); track segment.language) {
          <div
            class="stack__segment"
            [style.flex-basis.%]="segment.percentage"
            [style.background]="segment.color"
            [title]="segment.language + ': ' + segment.percentage.toFixed(1) + '% · ' + segment.files + ' archivos'"
          ></div>
        }
      </div>

      <ul class="legend">
        @for (segment of segments(); track segment.language) {
          <li class="legend__item">
            <span class="legend__swatch" [style.background]="segment.color" aria-hidden="true"></span>
            <span class="legend__name">{{ segment.language }}</span>
            <span class="legend__value">{{ segment.percentage.toFixed(1) }}%</span>
            <span class="legend__files">{{ segment.files }} arch.</span>
          </li>
        }
      </ul>
    }
  `,
  styleUrl: './language-composition.component.scss',
})
export class LanguageCompositionComponent {
  readonly languages = input.required<LanguageStat[]>();

  /** Maximo de segmentos con color propio antes de agrupar en "Otros". */
  private readonly maxSlots = CATEGORICAL_TOKENS.length;

  readonly segments = computed<Segment[]>(() => {
    const sorted = [...this.languages()].sort((a, b) => b.codeLines - a.codeLines);
    if (sorted.length === 0) {
      return [];
    }

    const named = sorted.slice(0, this.maxSlots).map((stat, index) => ({
      language: stat.language,
      percentage: stat.percentage,
      files: stat.files,
      codeLines: stat.codeLines,
      color: CATEGORICAL_TOKENS[index],
    }));

    const rest = sorted.slice(this.maxSlots);
    if (rest.length === 0) {
      return named;
    }

    return [
      ...named,
      {
        language: `Otros (${rest.length})`,
        percentage: rest.reduce((sum, stat) => sum + stat.percentage, 0),
        files: rest.reduce((sum, stat) => sum + stat.files, 0),
        codeLines: rest.reduce((sum, stat) => sum + stat.codeLines, 0),
        color: CATEGORICAL_OTHER,
      },
    ];
  });

  readonly ariaLabel = computed(() =>
    'Composicion por lenguaje: ' +
    this.segments()
      .map((segment) => `${segment.language} ${segment.percentage.toFixed(1)}%`)
      .join(', '),
  );
}
