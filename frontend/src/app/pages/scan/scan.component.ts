import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { folderNameOf } from '../../core/folder-archive';
import { EngineInfo } from '../../core/models/analysis.model';
import { Report } from '../../core/models/report.model';
import { AnalysisService } from '../../core/services/analysis.service';
import { ScanPipelineService, StepState } from '../../core/services/scan-pipeline.service';

type SourceTab = 'zip' | 'folder' | 'git';

/**
 * Pantalla principal: elegir de donde viene el codigo y lanzar el pipeline
 * source -> analysis -> report, viendo que microservicio trabaja en cada paso.
 */
@Component({
  selector: 'cl-scan',
  standalone: true,
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="head">
      <h1>Nuevo analisis</h1>
      <p class="head__lead">
        Carga tu proyecto desde un .zip, una carpeta de tu equipo o un repositorio remoto.
        CodeLens detecta vulnerabilidades, secretos expuestos, dependencias inseguras y problemas de salud del codigo.
      </p>
    </header>

    <div class="layout">
      <section class="card source" aria-labelledby="source-title">
        <h2 id="source-title" class="card__title">1 · Codigo fuente</h2>

        <div class="tabs" role="tablist" aria-label="Origen del codigo">
          @for (tab of tabs; track tab.id) {
            <button
              type="button"
              role="tab"
              class="tabs__tab"
              [class.tabs__tab--active]="activeTab() === tab.id"
              [attr.aria-selected]="activeTab() === tab.id"
              [disabled]="pipeline.running()"
              (click)="activeTab.set(tab.id)"
            >
              {{ tab.label }}
            </button>
          }
        </div>

        @switch (activeTab()) {
          @case ('zip') {
            <div
              class="drop"
              [class.drop--over]="dragOver()"
              (dragover)="onDragOver($event)"
              (dragleave)="dragOver.set(false)"
              (drop)="onDrop($event)"
            >
              <svg class="drop__icon" viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M12 15V3m0 0L7.5 7.5M12 3l4.5 4.5" /><path d="M4 15v4a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4" />
              </svg>
              @if (zipFile(); as file) {
                <p class="drop__file">{{ file.name }} <span class="muted">· {{ formatBytes(file.size) }}</span></p>
              } @else {
                <p class="drop__text">Suelta aqui tu archivo <strong>.zip</strong></p>
              }
              <label class="btn btn--ghost">
                {{ zipFile() ? 'Cambiar archivo' : 'Elegir .zip' }}
                <input class="visually-hidden" type="file" accept=".zip,application/zip" (change)="onZipPicked($event)" />
              </label>
            </div>
            <p class="field__hint">Hasta 200 MB. Se ignoran node_modules, builds y binarios.</p>
          }

          @case ('folder') {
            <div class="drop">
              <svg class="drop__icon" viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
              </svg>
              @if (folderFiles().length) {
                <p class="drop__file">{{ folderName() }} <span class="muted">· {{ folderFiles().length }} archivos</span></p>
              } @else {
                <p class="drop__text">Elige la carpeta del proyecto en tu equipo</p>
              }
              <label class="btn btn--ghost">
                {{ folderFiles().length ? 'Cambiar carpeta' : 'Elegir carpeta' }}
                <input class="visually-hidden" type="file" webkitdirectory multiple (change)="onFolderPicked($event)" />
              </label>
            </div>
            <p class="field__hint">
              La carpeta se empaqueta en tu navegador antes de subirla; dependencias y builds
              (node_modules, .git, target, dist...) no se envian.
            </p>
          }

          @case ('git') {
            <label class="field">
              <span class="field__label">URL del repositorio</span>
              <input class="input" type="url" placeholder="https://github.com/usuario/proyecto" [(ngModel)]="repositoryUrl" name="repositoryUrl" />
            </label>
            <div class="row">
              <label class="field">
                <span class="field__label">Rama o tag <span class="muted">(opcional)</span></span>
                <input class="input" placeholder="main" [(ngModel)]="branch" name="branch" />
              </label>
              <label class="field">
                <span class="field__label">Token de acceso <span class="muted">(repos privados)</span></span>
                <input class="input" type="password" autocomplete="off" [(ngModel)]="accessToken" name="accessToken" />
              </label>
            </div>
            <p class="field__hint">El token solo se usa para clonar; nunca se guarda.</p>
          }
        }

        <button class="btn start" type="button" [disabled]="!canStart()" (click)="start()">
          {{ pipeline.running() ? 'Analizando...' : 'Analizar codigo' }}
        </button>
      </section>

      <aside class="side">
        <section class="card" aria-labelledby="pipeline-title">
          <h2 id="pipeline-title" class="card__title">2 · Pipeline de microservicios</h2>
          <ol class="steps">
            @for (step of pipeline.steps(); track step.id) {
              <li class="step" [attr.data-state]="step.state">
                <span class="step__dot" aria-hidden="true">
                  @switch (step.state) {
                    @case ('done') { <svg viewBox="0 0 16 16" width="12" height="12"><path d="M3.5 8.5l3 3 6-7" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" /></svg> }
                    @case ('failed') { <svg viewBox="0 0 16 16" width="12" height="12"><path d="M4.5 4.5l7 7m0-7-7 7" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" /></svg> }
                    @case ('running') { <span class="spinner"></span> }
                  }
                </span>
                <div class="step__body">
                  <div class="step__label">{{ step.label }}</div>
                  <div class="step__service mono">{{ step.service }}</div>
                  <div class="step__detail">{{ step.detail || stateLabel(step.state) }}</div>
                </div>
              </li>
            }
          </ol>

          @if (pipeline.error()) {
            <div class="alert alert--error" role="alert">{{ pipeline.error() }}</div>
          }
        </section>

        @if (engine(); as info) {
          <section class="card" aria-labelledby="engine-title">
            <h2 id="engine-title" class="card__title">Cobertura del motor</h2>
            <dl class="engine">
              <div><dt>Reglas activas</dt><dd>{{ info.activeRules }}</dd></div>
              <div><dt>Lenguajes</dt><dd>{{ info.languageProfiles }}</dd></div>
              <div><dt>Avisos de dependencias</dt><dd>{{ info.advisories }}</dd></div>
              <div><dt>Analizadores</dt><dd>{{ info.activeAnalyzers.length }}</dd></div>
            </dl>
          </section>
        }
      </aside>
    </div>
  `,
  styleUrl: './scan.component.scss',
})
export class ScanComponent implements OnInit {
  protected readonly pipeline = inject(ScanPipelineService);
  private readonly analysisService = inject(AnalysisService);
  private readonly router = inject(Router);

  readonly tabs: { id: SourceTab; label: string }[] = [
    { id: 'zip', label: 'Archivo .zip' },
    { id: 'folder', label: 'Carpeta local' },
    { id: 'git', label: 'Repositorio Git' },
  ];

  readonly activeTab = signal<SourceTab>('zip');
  readonly dragOver = signal(false);
  readonly zipFile = signal<File | null>(null);
  readonly folderFiles = signal<File[]>([]);
  readonly folderName = computed(() => folderNameOf(this.folderFiles()));
  readonly engine = signal<EngineInfo | null>(null);

  repositoryUrl = '';
  branch = '';
  accessToken = '';

  ngOnInit(): void {
    // Si se vuelve a esta pantalla despues de un escaneo, se empieza limpio.
    if (!this.pipeline.running()) {
      this.pipeline.reset();
    }
    this.analysisService.engineInfo().subscribe({
      next: (info) => this.engine.set(info),
      error: () => this.engine.set(null),
    });
  }

  canStart(): boolean {
    if (this.pipeline.running()) {
      return false;
    }
    switch (this.activeTab()) {
      case 'zip':
        return this.zipFile() !== null;
      case 'folder':
        return this.folderFiles().length > 0;
      case 'git':
        return /^https?:\/\/.+/.test(this.repositoryUrl.trim());
    }
  }

  async start(): Promise<void> {
    let report: Report | null = null;
    switch (this.activeTab()) {
      case 'zip':
        report = await this.pipeline.runFromZip(this.zipFile()!);
        break;
      case 'folder':
        report = await this.pipeline.runFromFolder(this.folderFiles());
        break;
      case 'git':
        report = await this.pipeline.runFromGit({
          repositoryUrl: this.repositoryUrl.trim(),
          branch: this.branch.trim() || null,
          accessToken: this.accessToken.trim() || null,
        });
        break;
    }
    if (report) {
      this.router.navigate(['/reports', report.id]);
    }
  }

  onZipPicked(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.zipFile.set(input.files?.[0] ?? null);
    input.value = '';
  }

  onFolderPicked(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.folderFiles.set(Array.from(input.files ?? []));
    input.value = '';
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file && file.name.toLowerCase().endsWith('.zip')) {
      this.zipFile.set(file);
    }
  }

  stateLabel(state: StepState): string {
    return { pending: 'En espera', running: 'Trabajando...', done: 'Listo', failed: 'Fallo' }[state];
  }

  formatBytes(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  }
}
