import { Injectable } from '@angular/core';
import { User } from '../models/user.model';

const TOKEN_KEY = 'rbm_token';
const USER_KEY = 'rbm_user';

@Injectable({ providedIn: 'root' })
export class TokenService {

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  saveSession(token: string, user: User): void {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  getUser(): User | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) { return null; }
    try {
      return JSON.parse(raw) as User;
    } catch {
      return null;   // donnée corrompue : on la traite comme absente
    }
  }

  clear(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  /** Vrai si le token est absent, illisible ou expiré. */
  isExpired(): boolean {
    const token = this.getToken();
    if (!token) { return true; }
    try {
      const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const json = JSON.parse(atob(payload));
      return typeof json.exp !== 'number' || json.exp * 1000 <= Date.now();
    } catch {
      return true;
    }
  }
}