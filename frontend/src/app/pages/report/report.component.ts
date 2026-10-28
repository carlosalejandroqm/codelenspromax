import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { describeHttpError } from '../../core/http-error';
import { AnalysisDetail, Finding, Severity } from '../../core/models/analysis.model';
import { Report } from '../../core/models/report.model';
import { AnalysisService } from '../../core/services/analysis.service';
import { ReportService } from '../../core/services/report.service';
import { LanguageCompositionComponent } from '../../shared/language-composition.component';
import { MeterComponent } from '../../shared/meter.component';
import { SeverityBadgeComponent } from '../../shared/severity-badge.component';
import { SeverityBarsComponent } from '../../shared/severity-bars.component';
import { CATEGORY_LABEL, SEVERITY_META, SEVERITY_ORDER, scoreStatusToken } from '../../shared/severity';
import { StatTileComponent } from '../../shared/stat-tile.component';
import { VERDICT_META } from '../../shared/verdict';

/** Etiquetas legibles de los sub-puntajes de salud que calcula el motor. */
const SUB_SCORE_LABEL: Record<string, string> = {
  documentacion: 'Documentacion',
  complejidad: 'Complejidad',
  duplicacion: 'Duplicacion',
  tamano_archivos: 'Tamano de archivos',
  pruebas: 'Pruebas',
  deuda_declarada: 'Deuda declarada (TODO/FIXME)',
  anidamiento: 'Anidamiento',
};

/**
 * Resultado de un escaneo. Combina el reporte (report-service) con el detalle
 * del analisis y sus hallazgos (analysis-service).
 */
@Component({
  selector: 'cl-report',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    RouterLink,
    LanguageCompositionComponent,
    MeterComponent,
    SeverityBadgeComponent,
    SeverityBarsComponent,
    StatTileComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (loading()) {
      <div class="card empty">Cargando reporte...</div>
    } @else if (error()) {
      <div class="alert alert--error" role="alert">{{ error() }}</div>
      <p><a routerLink="/history">Volver al historial</a></p>
    } @else {
      @if (report(); as r) {
      <header class="head">
        <div class="head__main">
          <p class="head__eyebrow">Reporte · {{ r.generatedAt | date: 'd MMM y, HH:mm' }}</p>
          <h1>{{ r.projectName }}</h1>
          <span class="verdict" [style.--verdict]="verdict().token">
            <span class="verdict__dot" aria-hidden="true"></span>
            {{ verdict().label }}
          </span>
        </div>
        <div class="head__actions">
          <button type="button" class="btn btn--ghost" (click)="downloadMarkdown(r)" [disabled]="busy()">Descargar .md</button>
          <button type="button" class="btn btn--ghost" (click)="openHtml(r)" [disabled]="busy()">Reporte imprimible</button>
          <a routerLink="/" class="btn">Nuevo analisis</a>
        </div>
      </header>

      <section class="grid grid--scores">
        <div class="card grade">
          <span class="grade__label">Nota</span>
          <span class="grade__value" [style.color]="scoreStatusToken(r.overallScore)">{{ r.grade }}</span>
        </div>
        <div class="card meters">
          <cl-meter label="Puntaje global" [value]="r.overallScore" unit="/100" [showStatus]="true" />
          <cl-meter label="Seguridad" [value]="r.securityScore" unit="/100" [showStatus]="true" />
          <cl-meter label="Salud del codigo" [value]="r.healthScore" unit="/100" [showStatus]="true" />
        </div>
        @if (analysis(); as a) {
          <div class="card tiles">
            <cl-stat-tile label="Archivos analizados" [value]="a.filesAnalyzed" />
            <cl-stat-tile label="Lineas de codigo" [value]="(a.codeLines | number) ?? a.codeLines" />
            <cl-stat-tile label="Hallazgos" [value]="findings().length" />
            <cl-stat-tile label="Duracion" [value]="(a.durationMillis / 1000 | number: '1.1-1') ?? ''" unit="s" />
          </div>
        }
      </section>

      <section class="card summary">
        <h2 class="card__title">Resumen ejecutivo</h2>
        <p class="summary__text">{{ r.executiveSummary }}</p>
        <div class="grid grid--two">
          <div>
            <h3 class="summary__heading">Fortalezas</h3>
            @if (r.strengths.length) {
              <ul class="bullets bullets--good">
                @for (item of r.strengths; track $index) { <li>{{ item }}</li> }
              </ul>
            } @else {
              <p class="muted">Sin fortalezas destacables.</p>
            }
          </div>
          <div>
            <h3 class="summary__heading">Riesgos clave</h3>
            @if (r.keyRisks.length) {
              <ul class="bullets bullets--risk">
                @for (item of r.keyRisks; track $index) { <li>{{ item }}</li> }
              </ul>
            } @else {
              <p class="muted">No se identificaron riesgos clave.</p>
            }
          </div>
        </div>
      </section>

      @if (analysis(); as a) {
        <section class="grid grid--three">
          <div class="card">
            <h2 class="card__title">Hallazgos por severidad</h2>
            <cl-severity-bars [counts]="severityCounts()" />
          </div>
          <div class="card">
            <h2 class="card__title">Lenguajes</h2>
            <cl-language-composition [languages]="a.languages" />
          </div>
          <div class="card">
            <h2 class="card__title">Salud del codigo</h2>
            <div class="subscores">
              @for (score of subScores(); track score.key) {
                <cl-meter [label]="score.label" [value]="score.value" unit="/100" [precision]="0" />
              }
            </div>
          </div>
        </section>
      }

      <section class="card">
        <h2 class="card__title">Plan de accion · {{ r.recommendations.length }} recomendaciones</h2>
        @if (r.recommendations.length) {
          <ol class="recs">
            @for (rec of r.recommendations; track rec.rank) {
              <li class="rec">
                <div class="rec__head">
                  <span class="priority" [attr.data-priority]="rec.priority">{{ rec.priority }} · {{ rec.priorityLabel }}</span>
                  <h3 class="rec__title">{{ rec.title }}</h3>
                </div>
                <p class="rec__problem">{{ rec.problem }}</p>
                <p><strong>Accion:</strong> {{ rec.action }}</p>
                <p class="rec__why">{{ rec.rationale }}</p>
                <div class="rec__meta">
                  <span>{{ rec.timeWindow }}</span>
                  <span>Esfuerzo: {{ rec.effortEstimate }}</span>
                  @if (rec.ownerRole) { <span>Responsable: {{ rec.ownerRole }}</span> }
                  <span>{{ rec.findingsCount }} hallazgo(s)</span>
                  @if (rec.cwe) { <span class="mono">{{ rec.cwe }}</span> }
                  @if (rec.owasp) { <span class="mono">{{ rec.owasp }}</span> }
                </div>
                @if (rec.examples.length) {
                  <ul class="rec__examples">
                    @for (example of rec.examples; track $index) { <li class="mono">{{ example }}</li> }
                  </ul>
                }
                @if (rec.references.length) {
                  <div class="rec__refs">
                    @for (ref of rec.references; track ref) {
                      <a [href]="ref" target="_blank" rel="noopener">{{ shortUrl(ref) }}</a>
                    }
                  </div>
                }
              </li>
            }
          </ol>
        } @else {
          <p class="empty">No hay recomendaciones: el codigo no presenta hallazgos relevantes.</p>
        }
      </section>

      <section class="card">
        <div class="findings__head">
          <h2 class="card__title">Hallazgos · {{ filteredFindings().length }} de {{ findings().length }}</h2>
          <div class="filters">
            <label class="visually-hidden" for="f-sev">Severidad</label>
            <select id="f-sev" class="input" [ngModel]="severityFilter()" (ngModelChange)="severityFilter.set($event)">
              <option value="">Todas las severidades</option>
              @for (sev of severities; track sev) { <option [value]="sev">{{ severityLabel(sev) }}</option> }
            </select>
            <label class="visually-hidden" for="f-cat">Categoria</label>
            <select id="f-cat" class="input" [ngModel]="categoryFilter()" (ngModelChange)="categoryFilter.set($event)">
              <option value="">Todas las categorias</option>
              @for (cat of categories(); track cat) { <option [value]="cat">{{ categoryLabel(cat) }}</option> }
            </select>
            <label class="visually-hidden" for="f-text">Buscar</label>
            <input id="f-text" class="input" type="search" placeholder="Buscar archivo o regla" [ngModel]="textFilter()" (ngModelChange)="textFilter.set($event)" />
          </div>
        </div>

        @if (filteredFindings().length) {
          <div class="scroll-x">
            <table class="table">
              <thead>
                <tr>
                  <th>Severidad</th>
                  <th>Hallazgo</th>
                  <th>Ubicacion</th>
                  <th>Categoria</th>
                </tr>
              </thead>
              <tbody>
                @for (f of filteredFindings(); track f.id) {
                  <tr>
                    <td><cl-severity-badge [severity]="f.severity" /></td>
                    <td class="finding">
                      <details>
                        <summary>{{ f.title }}</summary>
                        <p>{{ f.description }}</p>
                        @if (f.evidence) { <pre class="evidence">{{ f.evidence }}</pre> }
                        @if (f.remediation) { <p><strong>Como corregirlo:</strong> {{ f.remediation }}</p> }
                        <p class="finding__meta mono">
                          {{ f.ruleId }}
                          @if (f.cwe) { · {{ f.cwe }} }
                          @if (f.owasp) { · {{ f.owasp }} }
                          · confianza {{ f.confidence * 100 | number: '1.0-0' }}%
                        </p>
                      </details>
                    </td>
                    <td class="mono location">{{ f.filePath ?? '—' }}@if (f.lineNumber > 0) {:{{ f.lineNumber }}}</td>
                    <td>{{ categoryLabel(f.category) }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        } @else {
          <p class="empty">Ningun hallazgo coincide con los filtros.</p>
        }
      </section>
      }
    }
  `,
  styleUrl: './report.component.scss',
})
export class ReportComponent {
  private readonly reportService = inject(ReportService);
  private readonly analysisService = inject(AnalysisService);

  /** Parametro de ruta :id (withComponentInputBinding). */
  readonly id = input.required<string>();

  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly report = signal<Report | null>(null);
  readonly detail = signal<AnalysisDetail | null>(null);

  readonly severityFilter = signal<Severity | ''>('');
  readonly categoryFilter = signal('');
  readonly textFilter = signal('');

  readonly severities = SEVERITY_ORDER;

  readonly analysis = computed(() => this.detail()?.analysis ?? null);
  readonly findings = computed<Finding[]>(() => this.detail()?.findings ?? []);

  readonly verdict = computed(() => {
    const verdict = this.report()?.verdict ?? 'RECHAZADO';
    return VERDICT_META[verdict] ?? { label: verdict, token: 'var(--text-muted)' };
  });

  readonly severityCounts = computed<Record<Severity, number>>(() => {
    const a = this.analysis();
    return {
      CRITICAL: a?.criticalCount ?? 0,
      HIGH: a?.highCount ?? 0,
      MEDIUM: a?.mediumCount ?? 0,
      LOW: a?.lowCount ?? 0,
      INFO: a?.infoCount ?? 0,
    };
  });

  readonly subScores = computed(() =>
    Object.entries(this.analysis()?.subScores ?? {})
      .map(([key, value]) => ({ key, label: SUB_SCORE_LABEL[key] ?? key, value }))
      .sort((a, b) => a.value - b.value),
  );

  readonly categories = computed(() => [...new Set(this.findings().map((f) => f.category))]);

  readonly filteredFindings = computed(() => {
    const severity = this.severityFilter();
    const category = this.categoryFilter();
    const text = this.textFilter().trim().toLowerCase();
    return this.findings()
      .filter((f) => !severity || f.severity === severity)
      .filter((f) => !category || f.category === category)
      .filter(
        (f) =>
          !text ||
          (f.filePath ?? '').toLowerCase().includes(text) ||
          f.title.toLowerCase().includes(text) ||
          f.ruleId.toLowerCase().includes(text),
      )
      .sort((a, b) => SEVERITY_META[b.severity].rank - SEVERITY_META[a.severity].rank);
  });

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => this.load(id));
    });
  }

  private async load(id: string): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const report = await firstValueFrom(this.reportService.get(id));
      this.report.set(report);
      this.detail.set(await firstValueFrom(this.analysisService.detail(report.analysisId)));
    } catch (error) {
      this.error.set(describeHttpError(error));
    } finally {
      this.loading.set(false);
    }
  }

  downloadMarkdown(report: Report): void {
    this.busy.set(true);
    this.reportService.markdown(report.id).subscribe({
      next: (markdown) => {
        const url = URL.createObjectURL(new Blob([markdown], { type: 'text/markdown;charset=utf-8' }));
        const link = document.createElement('a');
        link.href = url;
        link.download = `codelens-${this.slug(report.projectName)}.md`;
        link.click();
        URL.revokeObjectURL(url);
        this.busy.set(false);
      },
      error: (error) => {
        this.error.set(describeHttpError(error));
        this.busy.set(false);
      },
    });
  }

  /** La ventana se abre antes de la peticion para que el navegador no la bloquee. */
  openHtml(report: Report): void {
    const tab = window.open('', '_blank');
    this.busy.set(true);
    this.reportService.html(report.id).subscribe({
      next: (html) => {
        const url = URL.createObjectURL(new Blob([html], { type: 'text/html;charset=utf-8' }));
        if (tab) {
          tab.location.href = url;
        } else {
          window.location.href = url;
        }
        this.busy.set(false);
      },
      error: (error) => {
        tab?.close();
        this.error.set(describeHttpError(error));
        this.busy.set(false);
      },
    });
  }

  readonly scoreStatusToken = scoreStatusToken;

  severityLabel(severity: Severity): string {
    return SEVERITY_META[severity].label;
  }

  categoryLabel(category: string): string {
    return CATEGORY_LABEL[category] ?? category;
  }

  shortUrl(url: string): string {
    return url.replace(/^https?:\/\//, '').replace(/\/$/, '');
  }

  private slug(value: string): string {
    return value.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '') || 'reporte';
  }
}
