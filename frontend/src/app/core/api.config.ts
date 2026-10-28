import { InjectionToken } from '@angular/core';

/**
 * URL base de la API. Apunta al API Gateway, que enruta hacia los
 * microservicios; el frontend no conoce los puertos internos.
 *
 * Es relativa: en desarrollo `ng serve` la reenvia al gateway con
 * proxy.conf.json y en Docker lo hace nginx, asi que no hace falta CORS.
 *
 * Se inyecta como token para poder cambiarla por entorno sin tocar los
 * servicios que la consumen.
 */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => '/api/v1',
});
