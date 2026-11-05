import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.model';
import { RoleName, User } from '../models/user.model';
import { TokenService } from './token.service';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly url = `${environment.apiUrl}/auth`;
  private readonly userSubject = new BehaviorSubject<User | null>(this.initialUser());

  /** Utilisateur connecté, ou null. Les composants peuvent s'y abonner. */
  readonly user$: Observable<User | null> = this.userSubject.asObservable();

  constructor(
    private http: HttpClient,
    private tokens: TokenService,
    private router: Router
  ) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.url}/login`, request)
      .pipe(tap(res => this.startSession(res)));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.url}/register`, request)
      .pipe(tap(res => this.startSession(res)));
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
}