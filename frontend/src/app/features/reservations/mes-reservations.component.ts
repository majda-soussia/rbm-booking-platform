import { Component, OnInit } from '@angular/core';
import { Invitation } from '../../core/models/invitation.model';
import { Reservation, StatutReservation } from '../../core/models/reservation.model';
import { ReservationService } from '../../core/services/reservation.service';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

@Component({
  selector: 'app-mes-reservations',
  templateUrl: './mes-reservations.component.html',
  styleUrls: ['./mes-reservations.component.scss']
})
export class MesReservationsComponent implements OnInit {

  readonly filtres: { value: StatutReservation | 'TOUS'; label: string }[] = [
    { value: 'TOUS', label: 'Toutes' },
    { value: 'EN_ATTENTE', label: 'En attente' },
    { value: 'CONFIRMEE', label: 'Confirmées' },
    { value: 'REFUSEE', label: 'Refusées' },
    { value: 'ANNULEE', label: 'Annulées' }
  ];

  reservations: Reservation[] = [];
  filtre: StatutReservation | 'TOUS' = 'TOUS';
  loading = true;
  error = '';
  info = '';

  // Annulation : on demande une confirmation avant d'agir
  aAnnuler: number | null = null;
  annulationEnCours = false;

  // Invitations : un seul panneau ouvert à la fois
  ouvert: number | null = null;
  invitations: Invitation[] = [];
  loadingInvitations = false;
  saisie = '';
  envoiEnCours = false;
  invitationError = '';
  invitationInfo = '';

  constructor(private api: ReservationService) {}

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.api.mine().subscribe({
      next: list => { this.reservations = list; this.loading = false; },
      error: (err: Error) => { this.error = err.message; this.loading = false; }
    });
  }

  get visibles(): Reservation[] {
    return this.filtre === 'TOUS' ? this.reservations : this.reservations.filter(r => r.statut === this.filtre);
  }

  compte(statut: StatutReservation | 'TOUS'): number {
    return statut === 'TOUS' ? this.reservations.length : this.reservations.filter(r => r.statut === statut).length;
  }

  libelle(statut: StatutReservation): string {
    return { EN_ATTENTE: 'En attente', CONFIRMEE: 'Confirmée', REFUSEE: 'Refusée', ANNULEE: 'Annulée' }[statut];
  }

  // ---------- Annulation ----------

  peutAnnuler(r: Reservation): boolean {
    return (r.statut === 'EN_ATTENTE' || r.statut === 'CONFIRMEE') && new Date(r.dateDebut).getTime() > Date.now();
  }

  demanderAnnulation(id: number): void {
    this.aAnnuler = id;
    this.error = '';
    this.info = '';
  }

  confirmerAnnulation(r: Reservation): void {
    this.annulationEnCours = true;
    this.api.annuler(r.id).subscribe({
      next: maj => {
        this.reservations = this.reservations.map(x => x.id === maj.id ? maj : x);
        if (this.ouvert === r.id) { this.ouvert = null; }
        this.aAnnuler = null;
        this.annulationEnCours = false;
        this.info = `La réservation « ${r.titre} » a été annulée.`;
      },
      error: (err: Error) => {
        this.error = err.message;
        this.aAnnuler = null;
        this.annulationEnCours = false;
      }
    });
  }

  // ---------- Invitations ----------

  basculerInvitations(r: Reservation): void {
    if (this.ouvert === r.id) { this.ouvert = null; return; }
    this.ouvert = r.id;
    this.invitations = [];
    this.saisie = '';
    this.invitationError = '';
    this.invitationInfo = '';
    this.loadingInvitations = true;
    this.api.invitations(r.id).subscribe({
      next: list => { this.invitations = list; this.loadingInvitations = false; },
      error: (err: Error) => { this.invitationError = err.message; this.loadingInvitations = false; }
    });
  }

  /** Adresses saisies : séparées par virgule, point-virgule, espace ou retour à la ligne, sans doublon. */
  get adresses(): string[] {
    const toutes = this.saisie.split(/[\s,;]+/).map(a => a.trim().toLowerCase()).filter(a => a.length > 0);
    return Array.from(new Set(toutes));
  }

  get adressesInvalides(): string[] {
    return this.adresses.filter(a => !EMAIL_RE.test(a));
  }

  placesRestantes(r: Reservation): number {
    const deja = this.invitations.filter(i => i.statutEnvoi === 'ENVOYEE').length;
    return Math.max(0, r.nbParticipants - deja);
  }

  peutEnvoyer(r: Reservation): boolean {
    const n = this.adresses.length;
    return n > 0 && this.adressesInvalides.length === 0 && n <= this.placesRestantes(r) && !this.envoiEnCours;
  }

  envoyer(r: Reservation): void {
    if (!this.peutEnvoyer(r)) { return; }
    this.envoiEnCours = true;
    this.invitationError = '';
    this.invitationInfo = '';

    this.api.inviter(r.id, this.adresses.map(email => ({ email }))).subscribe({
      next: res => {
        this.invitations = [...this.invitations, ...res];
        const echecs = res.filter(i => i.statutEnvoi === 'ECHEC').length;
        this.invitationInfo = echecs === 0
          ? `${res.length} invitation${res.length > 1 ? 's' : ''} envoyée${res.length > 1 ? 's' : ''}.`
          : `${res.length - echecs} envoyée(s), ${echecs} en échec (voir l'historique ci-dessous).`;
        this.saisie = '';
        this.envoiEnCours = false;
      },
      error: (err: Error) => { this.invitationError = err.message; this.envoiEnCours = false; }
    });
  }

  trackById(_: number, r: Reservation): number {
    return r.id;
  }
}