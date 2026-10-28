import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { Report, ReportSummary } from '../models/report.model';

/** Cliente del microservicio 3: reportes y recomendaciones. */
@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  generate(analysisId: string): Observable<Report> {
    return this.http.post<Report>(`${this.baseUrl}/reports`, { analysisId });
  }

  get(id: string): Observable<Report> {
    return this.http.get<Report>(`${this.baseUrl}/reports/${id}`);
  }

  list(): Observable<ReportSummary[]> {
    return this.http.get<ReportSummary[]>(`${this.baseUrl}/reports`);
  }

  /** Markdown como texto plano, para descargarlo desde el navegador. */
  markdown(id: string): Observable<string> {
    return this.http.get(`${this.baseUrl}/reports/${id}/markdown`, { responseType: 'text' });
  }

  /**
   * Reporte HTML autocontenido. Se pide con HttpClient (y no como enlace) para
   * que lleve el token; la pantalla lo abre como Blob en una pestana nueva.
   */
  html(id: string): Observable<string> {
    return this.http.get(`${this.baseUrl}/reports/${id}/html`, { responseType: 'text' });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/reports/${id}`);
  }
}
