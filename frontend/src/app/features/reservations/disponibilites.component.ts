import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Equipement } from '../../core/models/equipement.model';
import { Salle } from '../../core/models/salle.model';
import { EquipementService } from '../../core/services/equipement.service';
import { SalleService } from '../../core/services/salle.service';
import { isPast, minutesBetween, timeSlots, toLocalIso, todayStr } from '../../core/utils/date.util';

@Component({
  selector: 'app-disponibilites',
  templateUrl: './disponibilites.component.html',
  styleUrls: ['./disponibilites.component.scss']
})
export class DisponibilitesComponent implements OnInit {

  readonly times = timeSlots();
  readonly minDate = todayStr();

  model = { date: '', debut: '09:00', fin: '11:00', capacite: null as number | null };
  equipements: Equipement[] = [];
  equipementIds = new Set<number>();

  resultats: Salle[] | null = null;   // null : aucune recherche lancée
  loading = false;
  error = '';

  constructor(
    private sallesApi: SalleService,
    private equipementsApi: EquipementService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.equipementsApi.getAll().subscribe({
      next: list => this.equipements = list,
      error: () => { /* les filtres d'équipements sont facultatifs */ }
    });
  }

  get dureeMinutes(): number {
    return minutesBetween(this.model.debut, this.model.fin);
  }

  /** Message de validation du créneau ('' si valide). */
  get validation(): string {
    if (!this.model.date) { return 'Choisissez une date.'; }
    if (isPast(this.model.date, this.model.debut)) { return 'Le début ne peut pas être dans le passé.'; }
    if (this.dureeMinutes <= 0) { return 'L\'heure de fin doit être après l\'heure de début.'; }
    if (this.dureeMinutes < 60) { return 'La durée minimale est de 1 heure.'; }
    return '';
  }

  toggleEquipement(id: number): void {
    if (this.equipementIds.has(id)) { this.equipementIds.delete(id); } else { this.equipementIds.add(id); }
  }

  rechercher(): void {
    if (this.validation) { this.error = this.validation; return; }
    this.error = '';
    this.loading = true;

    this.sallesApi.disponibles(
      toLocalIso(this.model.date, this.model.debut),
      toLocalIso(this.model.date, this.model.fin),
      this.model.capacite,
      Array.from(this.equipementIds)
    ).subscribe({
      next: salles => { this.resultats = salles; this.loading = false; },
      error: (err: Error) => { this.error = err.message; this.loading = false; }
    });
  }

  prixEstime(salle: Salle): number {
    return salle.tarifHoraire * (this.dureeMinutes / 60);
  }

  photo(salle: Salle): string | null {
    return this.sallesApi.photoUrl(salle);
  }

  reserver(salle: Salle): void {
    this.router.navigate(['/reservations/nouvelle'], {
      queryParams: {
        salleId: salle.id,
        date: this.model.date,
        debut: this.model.debut,
        fin: this.model.fin,
        nb: this.model.capacite || null
      }
    });
  }

  trackById(_: number, salle: Salle): number {
    return salle.id;
  }
}