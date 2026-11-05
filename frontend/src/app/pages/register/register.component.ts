import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent implements OnInit, OnDestroy {

  model = { prenom: '', nom: '', email: '', tel: '', password: '', confirm: '' };
  showPassword = false;
  loading = false;
  error = '';

  private readonly labels = ['Très faible', 'Faible', 'Moyen', 'Bon', 'Excellent'];

  constructor(private auth: AuthService, private router: Router) {}

  ngOnInit(): void {
    document.body.classList.add('auth-blue');
  }
  ngOnDestroy(): void {
    document.body.classList.remove('auth-blue');
  }

  /** Score de 0 à 4 : longueur, minuscules et majuscules, chiffre, symbole ou grande longueur. */
  get strength(): number {
    const p = this.model.password;
    if (!p) { return 0; }
    let score = 0;
    if (p.length >= 8) { score++; }
    if (/[a-z]/.test(p) && /[A-Z]/.test(p)) { score++; }
    if (/\d/.test(p)) { score++; }
    if (/[^A-Za-z0-9]/.test(p) || p.length >= 12) { score++; }
    return score;
  }

  get strengthLabel(): string {
    return this.model.password ? this.labels[this.strength] : '';
  }

  get passwordsMatch(): boolean {
    return this.model.password === this.model.confirm;
  }

  submit(): void {
    if (this.loading) { return; }
    if (!this.passwordsMatch) {
      this.error = 'Les deux mots de passe ne correspondent pas.';
      return;
    }
    this.loading = true;
    this.error = '';

    const { confirm, ...data } = this.model;
    this.auth.register({ ...data, tel: data.tel || undefined }).subscribe({
      next: () => this.router.navigateByUrl('/dashboard'),   // le backend connecte directement le nouveau client
      error: (err: Error) => {
        this.error = err.message;
        this.loading = false;
      }
    });
  }
}