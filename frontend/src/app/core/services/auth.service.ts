import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { RoleName, User } from '../models/user.model';
import { TokenService } from './token.service';
import {
  AuthResponse, LoginRequest, RegisterRequest, RegisterResponse,
  ProfileUpdateRequest, PasswordChangeRequest
} from '../models/auth.model';
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly url = `${environment.apiUrl}/auth`;
  private readonly userSubject = new BehaviorSubject<User | null>(this.initialUser());

  /** Utilisateur connecté, ou null. Les composants peuvent s'y abonner. */
  readonly user$: Observable<User | null> = this.userSubject.asObservable();

  constructor(private http: HttpClient, private tokens: TokenService, private router: Router) {
  // Déconnexion ou connexion dans un autre onglet : on se met à jour
  window.addEventListener('storage', (event: StorageEvent) => {
    if (event.key !== 'rbm_token') { return; }
    if (!event.newValue) {
      this.userSubject.next(null);
      this.router.navigate(['/login']);
    } else {
      this.userSubject.next(this.tokens.getUser());
    }
  });
}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.url}/login`, request)
      .pipe(tap(res => this.startSession(res)));
  }

  register(payload: any): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${environment.apiUrl}/auth/register`, payload).pipe(
      tap(res => {
        if (res.session) {
          // reprends ici ce que faisait l'ancien register pour enregistrer le token
        }
      })
    );
  }

  logout(redirect: boolean = true): void {
    this.tokens.clear();
    this.userSubject.next(null);
    if (redirect) {
      this.router.navigate(['/login']);
    }
  }

  get currentUser(): User | null {
    return this.userSubject.value;
  }

  get isLoggedIn(): boolean {
    return !!this.currentUser && !this.tokens.isExpired();
  }

  hasRole(role: RoleName): boolean {
    return !!this.currentUser && this.currentUser.roles.includes(role);
  }

  get isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  private startSession(res: AuthResponse): void {
    this.tokens.saveSession(res.token, res.user);
    this.userSubject.next(res.user);
  }

  /** Au démarrage : on reprend la session seulement si le token n'est pas expiré. */
  private initialUser(): User | null {
    if (this.tokens.isExpired()) {
      this.tokens.clear();
      return null;
    }
    return this.tokens.getUser();
  }
    updateProfile(request: ProfileUpdateRequest): Observable<User> {
    return this.http.put<User>(`${this.url}/me`, request).pipe(
      tap(user => {
        this.tokens.saveUser(user);
        this.userSubject.next(user);   // le nom affiché dans la barre du haut se met à jour
      })
    );
  }

  changePassword(request: PasswordChangeRequest): Observable<void> {
    return this.http.put<void>(`${this.url}/password`, request);
  }
   verifyEmail(token: string) {
    return this.http.post<{ message: string }>(`${environment.apiUrl}/auth/verify-email`, { token });
  }

  // AJOUT
  resendVerification(email: string) {
    return this.http.post<{ message: string }>(`${environment.apiUrl}/auth/resend-verification`, { email });
  }
}