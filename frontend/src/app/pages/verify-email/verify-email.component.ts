import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-verify-email',
  templateUrl: './verify-email.component.html',
  styleUrls: ['./verify-email.component.scss']
})
export class VerifyEmailComponent implements OnInit {
  etat: 'chargement' | 'succes' | 'erreur' = 'chargement';
  message = '';

  constructor(private route: ActivatedRoute, private auth: AuthService) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.etat = 'erreur';
      this.message = 'Lien incomplet : le jeton de vérification est manquant.';
      return;
    }
    this.auth.verifyEmail(token).subscribe({
      next: res => { this.etat = 'succes'; this.message = res.message; },
      error: (err: Error) => {
        this.etat = 'erreur';
        this.message = err.message || 'Lien invalide ou expiré.';
      }
    });
  }
}