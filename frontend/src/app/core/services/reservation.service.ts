import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Reservation, ReservationRequest } from '../models/reservation.model';

@Injectable({ providedIn: 'root' })
export class ReservationService {
  private readonly url = `${environment.apiUrl}/reservations`;

  constructor(private http: HttpClient) {}

  create(request: ReservationRequest): Observable<Reservation> {
    return this.http.post<Reservation>(this.url, request);
  }
}