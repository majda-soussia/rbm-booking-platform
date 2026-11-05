import { Component, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { Equipement } from '../../core/models/equipement.model';
import { Salle } from '../../core/models/salle.model';
import { AuthService } from '../../core/services/auth.service';
import { EquipementService } from '../../core/services/equipement.service';
import { SalleService } from '../../core/services/salle.service';

@Component({
  selector: 'app-salles-list',
  templateUrl: './salles-list.component.html',
  styleUrls: ['./salles-list.component.scss']
})
export class SallesListComponent implements OnInit {

  salles: Salle[] = [];
  equipements: Equipement[] = [];
  loading = true;
  error = '';

  texte = '';
  capaciteMin: number | null = null;
  equipementIds = new Set<number>();

  constructor(
    private sallesApi: SalleService,
    private equipementsApi: EquipementService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    forkJoin({
      salles: this.sallesApi.getAll(),
      equipements: this.equipementsApi.getAll()
    }).subscribe({
      next: ({ salles, equipements }) => {
        this.salles = salles;
        this.equipements = equipements;
        this.loading = false;
      },
      error: (err: Error) => {
        this.error = err.message;
        this.loading = false;
      }
    });
  }

  /** Un client ne voit que les salles actives ; l'admin voit tout. */
  get sallesFiltrees(): Salle[] {
    const texte = this.texte.trim().toLowerCase();
    return this.salles.filter(s =>
      (this.auth.isAdmin || s.active)
      && (!texte || s.nom.toLowerCase().includes(texte) || (s.description || '').toLowerCase().includes(texte))
      && (!this.capaciteMin || s.capacite >= this.capaciteMin)
      && Array.from(this.equipementIds).every(id => s.equipements.some(e => e.id === id))
    );
  }

  get filtresActifs(): boolean {
    return !!this.texte.trim() || !!this.capaciteMin || this.equipementIds.size > 0;
  }

  toggleEquipement(id: number): void {
    if (this.equipementIds.has(id)) {
      this.equipementIds.delete(id);
    } else {
      this.equipementIds.add(id);
    }
  }

  reinitialiser(): void {
    this.texte = '';
    this.capaciteMin = null;
    this.equipementIds.clear();
  }

  photo(salle: Salle): string | null {
    return this.sallesApi.photoUrl(salle);
  }

  trackById(_: number, salle: Salle): number {
    return salle.id;
  }
}