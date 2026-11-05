export type TypeReservation = 'REUNION' | 'FORMATION' | 'EVENEMENT';
export type StatutReservation = 'EN_ATTENTE' | 'CONFIRMEE' | 'REFUSEE' | 'ANNULEE';

export interface ReservationRequest {
  titre: string;
  description?: string;
  type: TypeReservation;
  dateDebut: string;   // "2026-12-01T14:00:00"
  dateFin: string;
  nbParticipants: number;
  salleId: number;
}

export interface Reservation {
  id: number;
  titre: string;
  description: string | null;
  type: TypeReservation;
  dateDebut: string;
  dateFin: string;
  nbParticipants: number;
  statut: StatutReservation;
  prixTotal: number;
  dateCreation: string;
  salleId: number;
  salleNom: string;
  userId: number;
  userNom: string;
}