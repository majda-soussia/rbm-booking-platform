import { Component, OnInit } from '@angular/core';
import { Equipement } from '../../core/models/equipement.model';
import { EquipementService } from '../../core/services/equipement.service';

@Component({
  selector: 'app-admin-equipements',
  templateUrl: './admin-equipements.component.html',
  styleUrls: ['./admin-equipements.component.scss']
})
export class AdminEquipementsComponent implements OnInit {

  equipements: Equipement[] = [];
  loading = true;
  error = '';
  info = '';

  formOuvert = false;
  editingId: number | null = null;
  model = { libelle: '', description: '' };
  submitted = false;
  saving = false;
  formError = '';

  aSupprimer: number | null = null;
  occupe: number | null = null;

  constructor(private api: EquipementService) {}

  ngOnInit(): void {
    this.api.getAll().subscribe({
      next: list => { this.equipements = list; this.loading = false; },
      error: (err: Error) => { this.error = err.message; this.loading = false; }
    });
  }

  ouvrirCreation(): void {
    this.editingId = null;
    this.model = { libelle: '', description: '' };
    this.ouvrir();
  }

  ouvrirEdition(e: Equipement): void {
    this.editingId = e.id;
    this.model = { libelle: e.libelle, description: e.description || '' };
    this.ouvrir();
  }

  private ouvrir(): void {
    this.formOuvert = true;
    this.submitted = false;
    this.formError = '';
    this.error = '';
    this.info = '';
  }

  fermer(): void {
    this.formOuvert = false;
  }

  enregistrer(valid: boolean | null): void {
    this.submitted = true;
    if (!valid || this.saving) { return; }

    const request = { libelle: this.model.libelle.trim(), description: this.model.description.trim() || null };
    const creation = this.editingId === null;
    this.saving = true;
    this.formError = '';

    const appel = creation ? this.api.create(request) : this.api.update(this.editingId as number, request);
    appel.subscribe({
      next: e => {
        this.equipements = creation
          ? [...this.equipements, e]
          : this.equipements.map(x => x.id === e.id ? e : x);
        this.info = creation ? `L'équipement « ${e.libelle} » a été créé.` : `L'équipement « ${e.libelle} » a été modifié.`;
        this.saving = false;
        this.formOuvert = false;
      },
      error: (err: Error) => { this.formError = err.message; this.saving = false; }
    });
  }

  demanderSuppression(id: number): void {
    this.aSupprimer = id;
    this.error = '';
    this.info = '';
  }

  supprimer(e: Equipement): void {
    this.occupe = e.id;
    this.api.delete(e.id).subscribe({
      next: () => {
        this.equipements = this.equipements.filter(x => x.id !== e.id);
        this.info = `L'équipement « ${e.libelle} » a été supprimé.`;
        this.aSupprimer = null;
        this.occupe = null;
      },
      error: (err: Error) => { this.error = err.message; this.aSupprimer = null; this.occupe = null; }
    });
  }

  trackById(_: number, e: Equipement): number {
    return e.id;
  }
}