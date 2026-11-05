import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

/** Erreur lisible par les écrans : message du backend + code HTTP. */
export class ApiError extends Error {
  constructor(message: string, public status: number) {
    super(message);
    Object.setPrototypeOf(this, ApiError.prototype);
  }
}

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {

  constructor(private auth: AuthService, private router: Router) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(req).pipe(
      catchError((err: HttpErrorResponse) => {
        const estAuth = req.url.includes('/auth/login') || req.url.includes('/auth/register');

        if (err.status === 401 && !estAuth) {
          this.auth.logout(false);
          this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
        }
        return throwError(() => new ApiError(this.messageDe(err), err.status));
      })
    );
  }

  private messageDe(err: HttpErrorResponse): string {
    if (err.status === 0) {
      return 'Serveur injoignable. Vérifiez que le backend est démarré.';
    }
    if (err.error && typeof err.error.message === 'string' && err.error.message) {
      return err.error.message;
    }
    switch (err.status) {
      case 401: return 'Session expirée ou non authentifiée.';
      case 403: return 'Accès refusé : vous n\'avez pas les droits nécessaires.';
      case 404: return 'Ressource introuvable.';
      default:  return 'Une erreur est survenue (code ' + err.status + ').';
    }
  }
}