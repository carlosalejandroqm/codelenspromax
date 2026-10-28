import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { Analysis, AnalysisDetail, EngineInfo, Finding } from '../models/analysis.model';

/** Cliente del microservicio 2: analisis de vulnerabilidades y salud. */
@Injectable({ providedIn: 'root' })
export class AnalysisService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  analyze(sourceId: string): Observable<Analysis> {
    return this.http.post<Analysis>(`${this.baseUrl}/analyses`, { sourceId });
  }

  get(id: string): Observable<Analysis> {
    return this.http.get<Analysis>(`${this.baseUrl}/analyses/${id}`);
  }

  detail(id: string): Observable<AnalysisDetail> {
    return this.http.get<AnalysisDetail>(`${this.baseUrl}/analyses/${id}/detail`);
  }

  findings(id: string): Observable<Finding[]> {
    return this.http.get<Finding[]>(`${this.baseUrl}/analyses/${id}/findings`);
  }

  list(): Observable<Analysis[]> {
    return this.http.get<Analysis[]>(`${this.baseUrl}/analyses`);
  }

  /** Cobertura del motor, para mostrarla en la pantalla de inicio. */
  engineInfo(): Observable<EngineInfo> {
    return this.http.get<EngineInfo>(`${this.baseUrl}/rules/info`);
  }
}
