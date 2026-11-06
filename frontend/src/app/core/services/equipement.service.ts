import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Equipement, EquipementRequest } from '../models/equipement.model';

@Injectable({ providedIn: 'root' })
export class EquipementService {
  private readonly url = `${environment.apiUrl}/equipements`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Equipement[]> {
    return this.http.get<Equipement[]>(this.url);
  }

  create(request: EquipementRequest): Observable<Equipement> {
    return this.http.post<Equipement>(this.url, request);
  }

  update(id: number, request: EquipementRequest): Observable<Equipement> {
    return this.http.put<Equipement>(`${this.url}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}