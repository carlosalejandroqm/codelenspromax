import { HttpErrorResponse } from '@angular/common/http';

/** Aprovecha el cuerpo de error uniforme (ApiError) de los microservicios. */
export function describeHttpError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    // 0: sin red/CORS. 502-504: el proxy (ng serve o nginx) no alcanza al gateway.
    if (error.status === 0 || error.status === 502 || error.status === 503 || error.status === 504) {
      return 'No hay conexion con el API Gateway. Verifica que los microservicios esten arriba (scripts/run-local.sh).';
    }
    const body = error.error as { message?: string } | string | null;
    if (body && typeof body === 'object' && body.message) {
      return body.message;
    }
    return `${error.status} ${error.statusText}`;
  }
  return error instanceof Error ? error.message : 'Error desconocido';
}
