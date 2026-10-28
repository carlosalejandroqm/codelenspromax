import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { archiveFolder } from '../folder-archive';
import { describeHttpError } from '../http-error';
import { Analysis } from '../models/analysis.model';
import { GitImportRequest, SourceSnapshot } from '../models/source.model';
import { Report } from '../models/report.model';
import { AnalysisService } from './analysis.service';
import { ReportService } from './report.service';
import { SourceService } from './source.service';

/** Los tres pasos del escaneo, uno por microservicio. */
export type PipelineStepId = 'source' | 'analysis' | 'report';

export type StepState = 'pending' | 'running' | 'done' | 'failed';

export interface PipelineStep {
  id: PipelineStepId;
  label: string;
  service: string;
  state: StepState;
  detail: string;
}

/**
 * Orquesta el escaneo completo desde el navegador: obtener el codigo, analizarlo
 * y generar el reporte.
 *
 * La orquestacion vive en el cliente a proposito: cada paso es una llamada
 * independiente a su microservicio, de modo que la pantalla puede mostrar que
 * servicio esta trabajando y donde falla si algo se cae.
 */
@Injectable({ providedIn: 'root' })
export class ScanPipelineService {
  private readonly sourceService = inject(SourceService);
  private readonly analysisService = inject(AnalysisService);
  private readonly reportService = inject(ReportService);

  private readonly stepsSignal = signal<PipelineStep[]>(this.initialSteps());
  private readonly runningSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);
  private readonly snapshotSignal = signal<SourceSnapshot | null>(null);
  private readonly analysisSignal = signal<Analysis | null>(null);
  private readonly reportSignal = signal<Report | null>(null);

  readonly steps = this.stepsSignal.asReadonly();
  readonly running = this.runningSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly snapshot = this.snapshotSignal.asReadonly();
  readonly analysis = this.analysisSignal.asReadonly();
  readonly report = this.reportSignal.asReadonly();

  readonly finished = computed(() => this.reportSignal() !== null);

  async runFromZip(file: File): Promise<Report | null> {
    return this.run(() => firstValueFrom(this.sourceService.importFromZip(file)));
  }

  async runFromGit(request: GitImportRequest): Promise<Report | null> {
    return this.run(() => firstValueFrom(this.sourceService.importFromGit(request)));
  }

  /** La carpeta se empaqueta en el navegador antes de subirla. */
  async runFromFolder(files: File[]): Promise<Report | null> {
    return this.run(async () => {
      this.setStep('source', 'running', `Empaquetando ${files.length} archivos de la carpeta...`);
      const archive = await archiveFolder(files);
      this.setStep(
        'source',
        'running',
        `Subiendo ${archive.includedFiles} archivos (${archive.skippedFiles} ignorados: dependencias o builds)...`,
      );
      return firstValueFrom(this.sourceService.importFromFolder(archive.blob, archive.name));
    });
  }

  reset(): void {
    this.stepsSignal.set(this.initialSteps());
    this.runningSignal.set(false);
    this.errorSignal.set(null);
    this.snapshotSignal.set(null);
    this.analysisSignal.set(null);
    this.reportSignal.set(null);
  }

  private async run(fetchSource: () => Promise<SourceSnapshot>): Promise<Report | null> {
    this.reset();
    this.runningSignal.set(true);

    try {
      this.setStep('source', 'running', 'Obteniendo el codigo...');
      const snapshot = await fetchSource();
      this.snapshotSignal.set(snapshot);
      this.setStep(
        'source',
        'done',
        `${snapshot.analyzableFiles} archivos analizables de ${snapshot.totalFiles}`,
      );

      this.setStep('analysis', 'running', 'Aplicando reglas y calculando metricas...');
      const analysis = await firstValueFrom(this.analysisService.analyze(snapshot.id));
      this.analysisSignal.set(analysis);
      const findings =
        analysis.criticalCount + analysis.highCount + analysis.mediumCount +
        analysis.lowCount + analysis.infoCount;
      this.setStep('analysis', 'done', `${findings} hallazgos · nota ${analysis.grade}`);

      this.setStep('report', 'running', 'Priorizando recomendaciones...');
      const report = await firstValueFrom(this.reportService.generate(analysis.id));
      this.reportSignal.set(report);
      this.setStep('report', 'done', `${report.recommendations.length} recomendaciones`);

      return report;
    } catch (error) {
      const message = this.describe(error);
      this.errorSignal.set(message);
      this.failCurrentStep(message);
      return null;
    } finally {
      this.runningSignal.set(false);
    }
  }

  private setStep(id: PipelineStepId, state: StepState, detail: string): void {
    this.stepsSignal.update((steps) =>
      steps.map((step) => (step.id === id ? { ...step, state, detail } : step)),
    );
  }

  /** Marca como fallido el paso que estaba en ejecucion. */
  private failCurrentStep(message: string): void {
    this.stepsSignal.update((steps) => {
      const index = steps.findIndex((step) => step.state === 'running');
      if (index < 0) {
        return steps;
      }
      const copy = [...steps];
      copy[index] = { ...copy[index], state: 'failed', detail: message };
      return copy;
    });
  }

  private describe(error: unknown): string {
    return describeHttpError(error);
  }

  private initialSteps(): PipelineStep[] {
    return [
      { id: 'source', label: 'Obtener codigo fuente', service: 'source-service :8081', state: 'pending', detail: '' },
      { id: 'analysis', label: 'Analizar vulnerabilidades y salud', service: 'analysis-service :8082', state: 'pending', detail: '' },
      { id: 'report', label: 'Generar reporte y recomendaciones', service: 'report-service :8083', state: 'pending', detail: '' },
    ];
  }
}
