export interface Equipement {
  id: number;
  libelle: string;
  description: string | null;
}
export interface EquipementRequest {
  libelle: string;
  description: string | null;
}