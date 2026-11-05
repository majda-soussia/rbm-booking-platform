import { User } from './user.model';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  tel?: string;
  nom: string;
  prenom: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  expiresInMs: number;
  user: User;
}