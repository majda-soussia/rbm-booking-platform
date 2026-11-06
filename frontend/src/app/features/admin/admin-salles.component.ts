import { Component, OnDestroy, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { Equipement } from '../../core/models/equipement.model';
import { Salle, SalleRequest } from '../../core/models/salle.model';
import { EquipementService } from '../../core/services/equipement.service';
import { SalleService } from '../../core/services/salle.service';

const MAX_PHOTO = 2 * 1024 * 1024;   // 2 Mo, comme le backend
const TYPES_PHOTO = ['image/jpeg', 'image/png'];

@Component({
  selector: 'app-admin-salles',
  templateUrl: './admin-salles.component.html',
  styleUrls: ['./admin-salles.component.scss']
})
export class AdminSallesComponent implements OnInit, OnDestroy {

  salles: Salle[] = [];
  equipements: Equipement[] = [];
  loading = true;
  error = '';
  info = '';

  // Formulaire
  formOuvert = false;
  editingId: number | null = null;     // null : création
  model = this.vide();
  equipementIds = new Set<number>();
  fichier: File | null = null;
  apercu: string | null = null;        // aperçu local de la photo choisie
  photoActuelle: string | null = null;
  photoError = '';
  submitted = false;
  saving = false;
  formError = '';

  // Suppression et activation
  aSupprimer: number | null = null;
  occupe: number | null = null;

  constructor(private api: SalleService, private equipementsApi: EquipementService) {}

  ngOnInit(): void {
    forkJoin({ salles: this.api.getAll(), equipements: this.equipementsApi.getAll() }).subscribe({
      next: ({ salles, equipements }) => {
        this.salles = salles;
        this.equipements = equipements;
        this.loading = false;
      },
      error: (err: Error) => { this.error = err.message; this.loading = false; }
    });
  }

  ngOnDestroy(): void {
    this.libererApercu();
  }

  private vide() {
    return {
      nom: '', capacite: 10, etage: null as number | null, description: '', adresse: '',
      localisationUrl: '', tarifHoraire: 50, active: true
    };
  }

  // ---------- Formulaire ----------

  ouvrirCreation(): void {
    this.reinitialiserFormulaire();
    this.editingId = null;
    this.formOuvert = true;
    this.demarrerEdition();
  }

  ouvrirEdition(s: Salle): void {
    this.reinitialiserFormulaire();
    this.editingId = s.id;
    this.model = {
      nom: s.nom, capacite: s.capacite, etage: s.etage, description: s.description || '',
      adresse: s.adresse || '', localisationUrl: s.localisationUrl || '',
      tarifHoraire: s.tarifHoraire, active: s.active
    };
    this.equipementIds = new Set(s.equipements.map(e => e.id));
    this.photoActuelle = this.api.photoUrl(s);
    this.formOuvert = true;
    this.demarrerEdition();
  }

  fermerFormulaire(): void {
    this.reinitialiserFormulaire();
    this.formOuvert = false;
  }

  private demarrerEdition(): void {
    this.error = '';
    this.info = '';
    setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }), 0);
  }

  private reinitialiserFormulaire(): void {
    this.libererApercu();
    this.model = this.vide();
    this.equipementIds = new Set<number>();
    this.fichier = null;
    this.photoActuelle = null;
    this.photoError = '';
    this.submitted = false;
    this.formError = '';
  }

  private libererApercu(): void {
    if (this.apercu) { URL.revokeObjectURL(this.apercu); this.apercu = null; }
  }

  toggleEquipement(id: number): void {
    if (this.equipementIds.has(id)) { this.equipementIds.delete(id); } else { this.equipementIds.add(id); }
  }

  choisirPhoto(event: Event): void {
    const input = event.target as HTMLInputElement;
    const f = input.files && input.files[0];
    this.photoError = '';
    this.libererApercu();
    this.fichier = null;
    if (!f) { return; }

    if (!TYPES_PHOTO.includes(f.type)) {
      this.photoError = 'Format non supporté : seuls JPG et PNG sont acceptés.';
      input.value = '';
      return;
    }
    if (f.size > MAX_PHOTO) {
      this.photoError = 'La photo ne doit pas dépasser 2 Mo.';
      input.value = '';
      return;
    }
    this.fichier = f;
    this.apercu = URL.createObjectURL(f);
  }

  get lienInvalide(): boolean {
    const l = this.model.localisationUrl.trim();
    return l.length > 0 && !/^https?:\/\/.+/.test(l);
  }

  enregistrer(valid: boolean | null): void {
    this.submitted = true;
    if (!valid || this.lienInvalide || this.saving) { return; }

    const request: SalleRequest = {
      nom: this.model.nom.trim(),
      capacite: this.model.capacite,
      etage: this.model.etage === null || (this.model.etage as unknown) === '' ? null : this.model.etage,
      description: this.model.description.trim() || null,
      adresse: this.model.adresse.trim() || null,
      localisationUrl: this.model.localisationUrl.trim() || null,
      tarifHoraire: this.model.tarifHoraire,
      active: this.model.active,
      equipementIds: Array.from(this.equipementIds)
    };

    this.saving = true;
    this.formError = '';
    const creation = this.editingId === null;
    const appel = creation ? this.api.create(request) : this.api.update(this.editingId as number, request);

    appel.subscribe({
      next: salle => {
        if (!this.fichier) { this.terminer(salle, creation); return; }

        this.api.uploadPhoto(salle.id, this.fichier).subscribe({
          next: avecPhoto => this.terminer(avecPhoto, creation),
          error: (err: Error) => {
            // La salle est enregistrée : seule la photo a échoué
            this.terminer(salle, creation);
            this.error = `La salle a été enregistrée, mais la photo n'a pas pu être envoyée : ${err.message}`;
          }
        });
      },
      error: (err: Error) => { this.formError = err.message; this.saving = false; }
    });
  }

  private terminer(salle: Salle, creation: boolean): void {
    this.salles = creation
      ? [...this.salles, salle]
      : this.salles.map(s => s.id === salle.id ? salle : s);
    this.info = creation ? `La salle « ${salle.nom} » a été créée.` : `La salle « ${salle.nom} » a été modifiée.`;
    this.saving = false;
    this.fermerFormulaire();
  }

  // ---------- Activation et suppression ----------

  basculerActive(s: Salle): void {
    this.occupe = s.id;
    this.error = '';
    this.info = '';
    this.api.setActive(s.id, !s.active).subscribe({
      next: maj => {
        this.salles = this.salles.map(x => x.id === maj.id ? maj : x);
        this.info = `La salle « ${maj.nom} » est maintenant ${maj.active ? 'active' : 'désactivée'}.`;
        this.occupe = null;
      },
      error: (err: Error) => { this.error = err.message; this.occupe = null; }
    });
  }

  demanderSuppression(id: number): void {
    this.aSupprimer = id;
    this.error = '';
    this.info = '';
  }

  supprimer(s: Salle): void {
    this.occupe = s.id;
    this.api.delete(s.id).subscribe({
      next: () => {
        this.salles = this.salles.filter(x => x.id !== s.id);
        this.info = `La salle « ${s.nom} » a été supprimée.`;
        this.aSupprimer = null;
        this.occupe = null;
      },
      error: (err: Error) => {
        this.error = err.message;   // ex. « Impossible de supprimer cette salle : elle a des réservations futures... »
        this.aSupprimer = null;
        this.occupe = null;
      }
    });
  }

  photo(s: Salle): string | null {
    return this.api.photoUrl(s);
  }

  trackById(_: number, s: Salle): number {
    return s.id;
  }
}