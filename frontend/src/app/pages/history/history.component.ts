import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { describeHttpError } from '../../core/http-error';
import { ReportSummary, Verdict } from '../../core/models/report.model';
import { ReportService } from '../../core/services/report.service';
import { scoreStatusToken } from '../../shared/severity';
import { VERDICT_META } from '../../shared/verdict';

/** Reportes del usuario autenticado (el gateway filtra por X-User-Id). */
@Component({
  selector: 'cl-history',
  standalone: true,
  imports: [DatePipe, DecimalPipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="head">
      <div>
        <h1>Historial</h1>
        <p class="head__lead">Tus analisis anteriores, del mas reciente al mas antiguo.</p>
      </div>
      <a routerLink="/" class="btn">Nuevo analisis</a>
    </header>

    <section class="card">
      @if (loading()) {
        <p class="empty">Cargando...</p>
      } @else if (error()) {
        <div class="alert alert--error" role="alert">{{ error() }}</div>
      } @else if (reports().length === 0) {
        <div class="empty">
          <p>Aun no tienes analisis.</p>
          <a routerLink="/" class="btn">Analizar mi primer proyecto</a>
        </div>
      } @else {
        <div class="scroll-x">
          <table class="table">
            <thead>
              <tr>
                <th>Proyecto</th>
                <th>Fecha</th>
                <th>Veredicto</th>
                <th class="num">Nota</th>
                <th class="num">Puntaje</th>
                <th class="num">Recomendaciones</th>
                <th><span class="visually-hidden">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              @for (r of reports(); track r.id) {
                <tr>
                  <td class="project"><a [routerLink]="['/reports', r.id]">{{ r.projectName }}</a></td>
                  <td>{{ r.generatedAt | date: 'd MMM y, HH:mm' }}</td>
                  <td>
                    <span class="verdict" [style.--verdict]="verdict(r.verdict).token">
                      <span class="verdict__dot" aria-hidden="true"></span>{{ verdict(r.verdict).label }}
                    </span>
                  </td>
                  <td class="num grade" [style.color]="scoreStatusToken(r.overallScore)">{{ r.grade }}</td>
                  <td class="num">{{ r.overallScore | number: '1.1-1' }}</td>
                  <td class="num">{{ r.recommendations }}</td>
                  <td><a [routerLink]="['/reports', r.id]">Ver</a></td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
  styleUrl: './history.component.scss',
})
export class HistoryComponent implements OnInit {
  private readonly reportService = inject(ReportService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly reports = signal<ReportSummary[]>([]);

  readonly scoreStatusToken = scoreStatusToken;

  ngOnInit(): void {
    this.reportService.list().subscribe({
      next: (reports) => {
        this.reports.set(reports);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(describeHttpError(error));
        this.loading.set(false);
      },
    });
  }

  verdict(verdict: Verdict): { label: string; token: string } {
    return VERDICT_META[verdict] ?? { label: verdict, token: 'var(--text-muted)' };
  }
}
