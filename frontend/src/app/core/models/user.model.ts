export type RoleName = 'ADMIN' | 'CLIENT';

export interface User {
  id: number;
  email: string;
  tel: string | null;
  nom: string;
  prenom: string;
  roles: RoleName[];
}
export interface UserRequest {
  email: string;
  password?: string;
  tel?: string | null;
  nom: string;
  prenom: string;
  roles: RoleName[];
}