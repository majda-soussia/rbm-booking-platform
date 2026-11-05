import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Salle } from '../models/salle.model';

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
}