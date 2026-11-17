import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

/** Erreur renvoyée par l'intercepteur : un Error avec, en plus, le statut et le code du serveur. */
interface ApiError extends Error {
  status?: number;
  code?: string;
}

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit, OnDestroy {

  model = { email: '', password: '' };
  loading = false;
  error = '';
  nonVerifie = false;
  infoMessage = '';
  showPassword = false;

  constructor(
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    document.body.classList.add('auth-blue');
  }

  ngOnDestroy(): void {
    document.body.classList.remove('auth-blue');
  }

  submit(): void {
    if (this.loading) { return; }
    this.loading = true;
    this.error = '';
    this.infoMessage = '';
    this.nonVerifie = false;

    this.auth.login(this.model).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigateByUrl(returnUrl && returnUrl.startsWith('/') ? returnUrl : '/dashboard');
      },
      error: (err: ApiError) => {
        this.error = err.message;
        this.loading = false;
        // Code envoyé par le serveur, ou à défaut le texte du message
        this.nonVerifie = err.code === 'EMAIL_NOT_VERIFIED' || /non vérifiée/i.test(err.message);
      }
    });
  }

  renvoyer(): void {
    const email = this.model.email.trim();
    if (!email) { return; }
    this.auth.resendVerification(email).subscribe({
      next: r => {
        this.infoMessage = r.message;
        this.nonVerifie = false;
        this.error = '';
      },
      error: (err: Error) => { this.error = err.message; }
    });
  }
}