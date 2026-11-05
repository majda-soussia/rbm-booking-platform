import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Equipement } from '../models/equipement.model';

@Injectable({ providedIn: 'root' })
export class EquipementService {
  private readonly url = `${environment.apiUrl}/equipements`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Equipement[]> {
    return this.http.get<Equipement[]>(this.url);
  }
}