import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { Reservation, StatutReservation } from '../../core/models/reservation.model';
import { ReservationService } from '../../core/services/reservation.service';

type Action = 'confirmer' | 'refuser' | 'annuler';

@Component({
  selector: 'app-admin-reservations',
  templateUrl: './admin-reservations.component.html',
  styleUrls: ['./admin-reservations.component.scss']
})
export class AdminReservationsComponent implements OnInit {

  readonly filtres: { value: StatutReservation | 'TOUS'; label: string }[] = [
    { value: 'TOUS', label: 'Toutes' },
    { value: 'EN_ATTENTE', label: 'En attente' },
    { value: 'CONFIRMEE', label: 'Confirmées' },
    { value: 'REFUSEE', label: 'Refusées' },
    { value: 'ANNULEE', label: 'Annulées' }
  ];

  private readonly textes: Record<Action, { question: string; bouton: string; style: string; fait: string }> = {
    confirmer: { question: 'Confirmer cette réservation ?', bouton: 'Oui, confirmer', style: 'btn-success', fait: 'confirmée' },
    refuser:   { question: 'Refuser cette demande ?',       bouton: 'Oui, refuser',   style: 'btn-danger',  fait: 'refusée' },
    annuler:   { question: 'Annuler cette réservation ?',   bouton: 'Oui, annuler',   style: 'btn-danger',  fait: 'annulée' }
  };

  reservations: Reservation[] = [];
  filtre: StatutReservation | 'TOUS' = 'TOUS';
  texte = '';
  loading = true;
  error = '';
  info = '';

  enAttente: { id: number; action: Action } | null = null;   // action en attente de confirmation
  occupe: number | null = null;                                // réservation en cours de traitement

  constructor(private api: ReservationService) {}

  ngOnInit(): void {
    this.api.all().subscribe({
      next: list => {
        this.reservations = list;
        // On ouvre directement sur les demandes à traiter, s'il y en a
        if (list.some(r => r.statut === 'EN_ATTENTE')) { this.filtre = 'EN_ATTENTE'; }
        this.loading = false;
      },
      error: (err: Error) => { this.error = err.message; this.loading = false; }
    });
  }

  /** Demandes en attente d'abord, puis par date de début. */
  get visibles(): Reservation[] {
    const t = this.texte.trim().toLowerCase();
    return this.reservations
      .filter(r => this.filtre === 'TOUS' || r.statut === this.filtre)
      .filter(r => !t || r.titre.toLowerCase().includes(t)
        || r.salleNom.toLowerCase().includes(t) || r.userNom.toLowerCase().includes(t))
      .sort((a, b) => {
        const pa = a.statut === 'EN_ATTENTE' ? 0 : 1;
        const pb = b.statut === 'EN_ATTENTE' ? 0 : 1;
        return pa - pb || new Date(a.dateDebut).getTime() - new Date(b.dateDebut).getTime();
      });
  }

  compte(statut: StatutReservation | 'TOUS'): number {
    return statut === 'TOUS' ? this.reservations.length : this.reservations.filter(r => r.statut === statut).length;
  }

  libelle(statut: StatutReservation): string {
    return { EN_ATTENTE: 'En attente', CONFIRMEE: 'Confirmée', REFUSEE: 'Refusée', ANNULEE: 'Annulée' }[statut];
  }

  actionsPour(r: Reservation): Action[] {
    if (r.statut === 'EN_ATTENTE') { return ['confirmer', 'refuser', 'annuler']; }
    if (r.statut === 'CONFIRMEE') { return ['annuler']; }
    return [];
  }

  texteAction(action: Action) {
    return this.textes[action];
  }

  demander(r: Reservation, action: Action): void {
    this.enAttente = { id: r.id, action };
    this.error = '';
    this.info = '';
  }

  executer(r: Reservation): void {
    if (!this.enAttente || this.enAttente.id !== r.id) { return; }
    const action = this.enAttente.action;
    this.occupe = r.id;

    const appel: Observable<Reservation> =
      action === 'confirmer' ? this.api.confirmer(r.id)
      : action === 'refuser' ? this.api.refuser(r.id)
      : this.api.annuler(r.id);

    appel.subscribe({
      next: maj => {
        this.reservations = this.reservations.map(x => x.id === maj.id ? maj : x);
        this.info = `La réservation « ${r.titre} » a été ${this.textes[action].fait}.`;
        this.enAttente = null;
        this.occupe = null;
      },
      error: (err: Error) => {
        this.error = err.message;   // ex. « Salle déjà réservée sur ce créneau »
        this.enAttente = null;
        this.occupe = null;
      }
    });
  }

  trackById(_: number, r: Reservation): number {
    return r.id;
  }
}