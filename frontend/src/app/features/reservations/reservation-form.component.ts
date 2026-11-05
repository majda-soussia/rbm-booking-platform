import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Reservation, TypeReservation } from '../../core/models/reservation.model';
import { Salle } from '../../core/models/salle.model';
import { ReservationService } from '../../core/services/reservation.service';
import { SalleService } from '../../core/services/salle.service';
import { isPast, minutesBetween, timeSlots, toLocalIso, todayStr } from '../../core/utils/date.util';

@Component({
  selector: 'app-reservation-form',
  templateUrl: './reservation-form.component.html',
  styleUrls: ['./reservation-form.component.scss']
})
export class ReservationFormComponent implements OnInit {

  readonly times = timeSlots();
  readonly minDate = todayStr();
  readonly types: { value: TypeReservation; label: string }[] = [
    { value: 'REUNION', label: 'Réunion' },
    { value: 'FORMATION', label: 'Formation' },
    { value: 'EVENEMENT', label: 'Événement' }
  ];

  salle: Salle | null = null;
  loadingSalle = true;
  loadError = '';

  model = {
    titre: '', description: '', type: 'REUNION' as TypeReservation,
    date: '', debut: '09:00', fin: '11:00', nbParticipants: 1
  };

  submitted = false;
  saving = false;
  error = '';
  created: Reservation | null = null;

  constructor(
    private route: ActivatedRoute,
    private sallesApi: SalleService,
    private reservationsApi: ReservationService
  ) {}

  ngOnInit(): void {
    const q = this.route.snapshot.queryParamMap;
    this.model.date = q.get('date') || '';
    this.model.debut = q.get('debut') || this.model.debut;
    this.model.fin = q.get('fin') || this.model.fin;
    this.model.nbParticipants = Number(q.get('nb')) || 1;

    const salleId = Number(q.get('salleId'));
    if (!salleId) {
      this.loadError = 'Aucune salle sélectionnée. Choisissez une salle depuis la recherche de disponibilité.';
      this.loadingSalle = false;
      return;
    }
    this.sallesApi.getById(salleId).subscribe({
      next: s => { this.salle = s; this.loadingSalle = false; },
      error: (err: Error) => { this.loadError = err.message; this.loadingSalle = false; }
    });
  }

  get dureeMinutes(): number {
    return minutesBetween(this.model.debut, this.model.fin);
  }

  /** Premier problème détecté sur le créneau ou le nombre de participants ('' si tout est valide). */
  get probleme(): string {
    if (!this.model.date) { return 'Choisissez une date.'; }
    if (isPast(this.model.date, this.model.debut)) { return 'Le début ne peut pas être dans le passé.'; }
    if (this.dureeMinutes <= 0) { return 'L\'heure de fin doit être après l\'heure de début.'; }
    if (this.dureeMinutes < 60) { return 'La durée minimale est de 1 heure.'; }
    if (this.salle && !this.salle.active) { return 'Cette salle est désactivée et ne peut pas être réservée.'; }
    if (this.salle && this.model.nbParticipants > this.salle.capacite) {
      return `Cette salle accueille ${this.salle.capacite} personnes au maximum.`;
    }
    return '';
  }

  /** Aperçu du prix : durée en heures x tarif horaire. Le backend recalcule la valeur définitive. */
  get prixApercu(): number | null {
    if (!this.salle || this.dureeMinutes < 60) { return null; }
    return this.salle.tarifHoraire * (this.dureeMinutes / 60);
  }

  photo(): string | null {
    return this.salle ? this.sallesApi.photoUrl(this.salle) : null;
  }

  submit(valid: boolean | null): void {
    this.submitted = true;
    if (!valid || !this.salle || this.probleme || this.saving) { return; }

    this.saving = true;
    this.error = '';
    this.reservationsApi.create({
      titre: this.model.titre.trim(),
      description: this.model.description.trim() || undefined,
      type: this.model.type,
      dateDebut: toLocalIso(this.model.date, this.model.debut),
      dateFin: toLocalIso(this.model.date, this.model.fin),
      nbParticipants: this.model.nbParticipants,
      salleId: this.salle.id
    }).subscribe({
      next: r => { this.created = r; this.saving = false; },
      error: (err: Error) => { this.error = err.message; this.saving = false; }
    });
  }
}