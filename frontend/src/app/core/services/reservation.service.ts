import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CalendrierEvent } from '../models/calendrier.model';
import { Invitation, InviteRequest } from '../models/invitation.model';
import { Reservation, ReservationRequest } from '../models/reservation.model';

@Injectable({ providedIn: 'root' })
export class ReservationService {
  private readonly url = `${environment.apiUrl}/reservations`;

  constructor(private http: HttpClient) {}

  create(request: ReservationRequest): Observable<Reservation> {
    return this.http.post<Reservation>(this.url, request);
  }

  /** Réservations de l'utilisateur connecté (le backend lit l'identité dans le token). */
  mine(): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(`${this.url}/mes`);
  }

  /** Toutes les réservations (réservé à l'admin côté backend). */
  all(): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(this.url);
  }

  confirmer(id: number): Observable<Reservation> {
    return this.http.put<Reservation>(`${this.url}/${id}/confirmer`, {});
  }

  refuser(id: number): Observable<Reservation> {
    return this.http.put<Reservation>(`${this.url}/${id}/refuser`, {});
  }

  annuler(id: number): Observable<Reservation> {
    return this.http.put<Reservation>(`${this.url}/${id}/annuler`, {});
  }

  invitations(id: number): Observable<Invitation[]> {
    return this.http.get<Invitation[]>(`${this.url}/${id}/invitations`);
  }

  inviter(id: number, invites: InviteRequest[]): Observable<Invitation[]> {
    return this.http.post<Invitation[]>(`${this.url}/${id}/invitations`, { invites });
  }

  calendrier(debut: string, fin: string): Observable<CalendrierEvent[]> {
    const params = new HttpParams().set('debut', debut).set('fin', fin);
    return this.http.get<CalendrierEvent[]>(`${this.url}/calendrier`, { params });
  }
}