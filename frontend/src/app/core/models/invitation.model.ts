export type StatutEnvoi = 'ENVOYEE' | 'ECHEC';

export interface Invitation {
  id: number;
  emailInvite: string;
  nomInvite: string | null;
  statutEnvoi: StatutEnvoi;
  dateEnvoi: string;
  reservationId: number;
}

export interface InviteRequest {
  email: string;
  nom?: string;
}