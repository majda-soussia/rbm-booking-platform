import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Salle } from '../models/salle.model';
import { HttpClient, HttpParams } from '@angular/common/http';

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

  /** Les photos sont servies par le backend : on préfixe l'URL relative. */
  photoUrl(salle: Salle): string | null {
    return salle.photoUrl ? `${environment.baseUrl}${salle.photoUrl}` : null;
  }
  disponibles(debut: string, fin: string, capacite: number | null, equipementIds: number[]): Observable<Salle[]> {
    let params = new HttpParams().set('debut', debut).set('fin', fin);
    if (capacite) { params = params.set('capacite', String(capacite)); }
    equipementIds.forEach(id => { params = params.append('equipementIds', String(id)); });
    return this.http.get<Salle[]>(`${this.url}/disponibles`, { params });
  }
}