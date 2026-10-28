/** Usuario autenticado, tal como lo devuelve el user-service. */
export interface User {
  id: string;
  username: string;
  email: string;
  fullName: string;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresAt: string;
  user: User;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  fullName: string;
  password: string;
}
