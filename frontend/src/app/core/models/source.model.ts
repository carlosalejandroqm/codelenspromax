/** Estados por los que pasa un snapshot en el source-service. */
export type SnapshotStatus = 'PENDING' | 'FETCHING' | 'READY' | 'FAILED' | 'PURGED';

export type SourceType = 'GIT' | 'ZIP';

/** Snapshot de codigo obtenido, es el punto de partida del analisis. */
export interface SourceSnapshot {
  id: string;
  type: SourceType;
  status: SnapshotStatus;
  projectName: string;
  repositoryUrl: string | null;
  branch: string | null;
  commitId: string | null;
  totalFiles: number;
  analyzableFiles: number;
  skippedFiles: number;
  totalBytes: number;
  totalLines: number;
  errorMessage: string | null;
  createdAt: string;
  completedAt: string | null;
}

/** Peticion de importacion de un repositorio remoto. */
export interface GitImportRequest {
  repositoryUrl: string;
  branch?: string | null;
  accessToken?: string | null;
}
