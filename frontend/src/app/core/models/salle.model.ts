import { Equipement } from './equipement.model';

export interface Salle {
  id: number;
  nom: string;
  capacite: number;
  etage: number | null;
  description: string | null;
  adresse: string | null;
  localisationUrl: string | null;
  photoUrl: string | null;      // ex. /uploads/salles/salle-1-xxx.jpg
  tarifHoraire: number;
  active: boolean;
  equipements: Equipement[];
}