import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Salle, SalleRequest } from '../models/salle.model';

@Injectable({ providedIn: 'root' })
export class SalleService {
  private readonly url = `${environment.apiUrl}/salles`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Salle[]> {
    return this.http.get<Salle[]>(this.url);
  }

  getById(id: number): Observable<Salle> {
    return this.http.get<Salle>(`${this.url}/${id}`);
  }

  disponibles(debut: string, fin: string, capacite: number | null, equipementIds: number[]): Observable<Salle[]> {
    let params = new HttpParams().set('debut', debut).set('fin', fin);
    if (capacite) { params = params.set('capacite', String(capacite)); }
    equipementIds.forEach(id => { params = params.append('equipementIds', String(id)); });
    return this.http.get<Salle[]>(`${this.url}/disponibles`, { params });
  }

  // ---------- Administration ----------

  create(request: SalleRequest): Observable<Salle> {
    return this.http.post<Salle>(this.url, request);
  }

  update(id: number, request: SalleRequest): Observable<Salle> {
    return this.http.put<Salle>(`${this.url}/${id}`, request);
  }

  setActive(id: number, active: boolean): Observable<Salle> {
    return this.http.patch<Salle>(`${this.url}/${id}/active`, {}, { params: { value: String(active) } });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  /** Envoi multipart : le navigateur fixe lui-même l'en-tête Content-Type avec sa frontière. */
  uploadPhoto(id: number, file: File): Observable<Salle> {
    const data = new FormData();
    data.append('file', file);
    return this.http.post<Salle>(`${this.url}/${id}/photo`, data);
  }

  /** Les photos sont servies par le backend : on préfixe l'URL relative. */
  photoUrl(salle: Salle): string | null {
    return salle.photoUrl ? `${environment.baseUrl}${salle.photoUrl}` : null;
  }
}