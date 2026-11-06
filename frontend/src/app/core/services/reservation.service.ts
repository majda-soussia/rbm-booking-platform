import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
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

  annuler(id: number): Observable<Reservation> {
    return this.http.put<Reservation>(`${this.url}/${id}/annuler`, {});
  }

  invitations(id: number): Observable<Invitation[]> {
    return this.http.get<Invitation[]>(`${this.url}/${id}/invitations`);
  }

  inviter(id: number, invites: InviteRequest[]): Observable<Invitation[]> {
    return this.http.post<Invitation[]>(`${this.url}/${id}/invitations`, { invites });
  }
}