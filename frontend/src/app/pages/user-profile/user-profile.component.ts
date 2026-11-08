import { Component, OnInit } from '@angular/core';
import { Reservation } from '../../core/models/reservation.model';
import { User } from '../../core/models/user.model';
import { AuthService } from '../../core/services/auth.service';
import { ReservationService } from '../../core/services/reservation.service';

@Component({
  selector: 'app-user-profile',
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.scss']
})
export class UserProfileComponent implements OnInit {

  reservations: Reservation[] = [];

  // Identité
  edition = false;
  model = { prenom: '', nom: '', tel: '' };
  saving = false;
  profilError = '';
  profilInfo = '';

  // Mot de passe
  pwd = { actuel: '', nouveau: '', confirmation: '' };
  showPwd = false;
  pwdSubmitted = false;
  pwdSaving = false;
  pwdError = '';
  pwdInfo = '';

  constructor(public auth: AuthService, private reservationsApi: ReservationService) {}

  ngOnInit(): void {
    if (!this.auth.isAdmin) {
      this.reservationsApi.mine().subscribe({
        next: list => this.reservations = list,
        error: () => { /* le résumé est facultatif */ }
      });
    }
  }

  get user(): User | null {
    return this.auth.currentUser;
  }

  get initiales(): string {
    const u = this.user;
    return u ? `${u.prenom.charAt(0)}${u.nom.charAt(0)}`.toUpperCase() : '';
  }

  // ---------- Résumé client ----------

  get nbTotal(): number { return this.reservations.length; }

  get nbAVenir(): number {
    return this.reservations.filter(r =>
      (r.statut === 'CONFIRMEE' || r.statut === 'EN_ATTENTE') && new Date(r.dateDebut).getTime() > Date.now()).length;
  }

  get nbEnAttente(): number {
    return this.reservations.filter(r => r.statut === 'EN_ATTENTE').length;
  }

  // ---------- Identité ----------

  ouvrirEdition(): void {
    const u = this.user;
    if (!u) { return; }
    this.model = { prenom: u.prenom, nom: u.nom, tel: u.tel || '' };
    this.edition = true;
    this.profilError = '';
    this.profilInfo = '';
  }

  annulerEdition(): void {
    this.edition = false;
  }

  enregistrer(valid: boolean | null): void {
    if (!valid || this.saving) { return; }
    this.saving = true;
    this.profilError = '';

    this.auth.updateProfile({
      prenom: this.model.prenom.trim(),
      nom: this.model.nom.trim(),
      tel: this.model.tel.trim() || null
    }).subscribe({
      next: () => {
        this.saving = false;
        this.edition = false;
        this.profilInfo = 'Vos informations ont été mises à jour.';
      },
      error: (err: Error) => { this.profilError = err.message; this.saving = false; }
    });
  }

  // ---------- Mot de passe ----------

  get confirmationDifferente(): boolean {
    return this.pwd.confirmation.length > 0 && this.pwd.nouveau !== this.pwd.confirmation;
  }

  changerMotDePasse(valid: boolean | null): void {
    this.pwdSubmitted = true;
    if (!valid || this.pwd.nouveau.length < 8 || this.confirmationDifferente || this.pwdSaving) { return; }

    this.pwdSaving = true;
    this.pwdError = '';
    this.pwdInfo = '';

    this.auth.changePassword({ currentPassword: this.pwd.actuel, newPassword: this.pwd.nouveau }).subscribe({
      next: () => {
        this.pwd = { actuel: '', nouveau: '', confirmation: '' };
        this.pwdSubmitted = false;
        this.pwdSaving = false;
        this.pwdInfo = 'Votre mot de passe a été modifié.';
      },
      error: (err: Error) => { this.pwdError = err.message; this.pwdSaving = false; }
    });
  }
}