/** Cuerpo de error uniforme que devuelven los tres microservicios. */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
