import { StatutReservation } from './reservation.model';

export interface CalendrierEvent {
  id: number | null;          // null pour un créneau masqué
  salleId: number;
  salleNom: string;
  dateDebut: string;
  dateFin: string;
  statut: StatutReservation;
  titre: string;              // "Occupé" pour un créneau masqué
  clientNom: string | null;   // null pour un créneau masqué
  mine: boolean;
}