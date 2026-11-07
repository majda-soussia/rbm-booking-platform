import { Component, OnInit } from '@angular/core';
import { Dashboard } from '../../core/models/dashboard.model';
import { Reservation } from '../../core/models/reservation.model';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { ReservationService } from '../../core/services/reservation.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {

  stats: Dashboard | null = null;           // administrateur
  mesReservations: Reservation[] = [];       // client
  loading = true;
  error = '';

  constructor(
    public auth: AuthService,
    private dashboardApi: DashboardService,
    private reservationsApi: ReservationService
  ) {}

  ngOnInit(): void {
    if (this.auth.isAdmin) {
      this.dashboardApi.compteurs().subscribe({
        next: s => { this.stats = s; this.loading = false; },
        error: (err: Error) => { this.error = err.message; this.loading = false; }
      });
    } else {
      this.reservationsApi.mine().subscribe({
        next: list => { this.mesReservations = list; this.loading = false; },
        error: (err: Error) => { this.error = err.message; this.loading = false; }
      });
    }
  }

  // ---------- Administrateur ----------

  get totalReservations(): number {
    const s = this.stats;
    return s ? s.reservationsEnAttente + s.reservationsConfirmees + s.reservationsRefusees + s.reservationsAnnulees : 0;
  }

  /** Pourcentage d'une part du total, pour les barres (0 si aucune réservation). */
  part(valeur: number): number {
    return this.totalReservations ? Math.round((valeur * 100) / this.totalReservations) : 0;
  }

  // ---------- Client ----------

  private get aVenir(): Reservation[] {
    return this.mesReservations
      .filter(r => (r.statut === 'CONFIRMEE' || r.statut === 'EN_ATTENTE') && new Date(r.dateDebut).getTime() > Date.now())
      .sort((a, b) => new Date(a.dateDebut).getTime() - new Date(b.dateDebut).getTime());
  }

  get prochaine(): Reservation | null {
    return this.aVenir.length ? this.aVenir[0] : null;
  }

  get nbAVenir(): number { return this.aVenir.length; }

  get nbEnAttente(): number {
    return this.mesReservations.filter(r => r.statut === 'EN_ATTENTE').length;
  }

  get nbConfirmees(): number {
    return this.mesReservations.filter(r => r.statut === 'CONFIRMEE').length;
  }

  libelle(statut: string): string {
    return ({ EN_ATTENTE: 'En attente', CONFIRMEE: 'Confirmée', REFUSEE: 'Refusée', ANNULEE: 'Annulée' } as Record<string, string>)[statut];
  }
}