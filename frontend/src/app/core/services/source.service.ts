import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { GitImportRequest, SourceSnapshot } from '../models/source.model';

/** Cliente del microservicio 1: obtencion del codigo fuente. */
@Injectable({ providedIn: 'root' })
export class SourceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  importFromGit(request: GitImportRequest): Observable<SourceSnapshot> {
    return this.http.post<SourceSnapshot>(`${this.baseUrl}/sources/git`, request);
  }

  importFromZip(file: File): Observable<SourceSnapshot> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<SourceSnapshot>(`${this.baseUrl}/sources/zip`, form);
  }

  /** Carpeta local ya empaquetada en el navegador (ver folder-archive.ts). */
  importFromFolder(archive: Blob, name: string): Observable<SourceSnapshot> {
    const form = new FormData();
    form.append('file', archive, `${name}.zip`);
    form.append('name', name);
    return this.http.post<SourceSnapshot>(`${this.baseUrl}/sources/folder`, form);
  }

  get(id: string): Observable<SourceSnapshot> {
    return this.http.get<SourceSnapshot>(`${this.baseUrl}/sources/${id}`);
  }

  list(): Observable<SourceSnapshot[]> {
    return this.http.get<SourceSnapshot[]>(`${this.baseUrl}/sources`);
  }

  purge(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/sources/${id}`);
  }
}
