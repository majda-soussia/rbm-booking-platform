import { Component, OnInit } from '@angular/core';
import { RoleName, User, UserRequest } from '../../core/models/user.model';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

@Component({
  selector: 'app-admin-utilisateurs',
  templateUrl: './admin-utilisateurs.component.html',
  styleUrls: ['./admin-utilisateurs.component.scss']
})
export class AdminUtilisateursComponent implements OnInit {

  readonly rolesDispo: { value: RoleName; label: string }[] = [
    { value: 'CLIENT', label: 'Client' },
    { value: 'ADMIN', label: 'Administrateur' }
  ];

  users: User[] = [];
  texte = '';
  loading = true;
  error = '';
  info = '';

  formOuvert = false;
  editingId: number | null = null;
  model = this.vide();
  roles = new Set<RoleName>(['CLIENT']);
  submitted = false;
  saving = false;
  formError = '';

  aSupprimer: number | null = null;
  occupe: number | null = null;

  constructor(private api: UserService, public auth: AuthService) {}

  ngOnInit(): void {
    this.api.getAll().subscribe({
      next: list => { this.users = list; this.loading = false; },
      error: (err: Error) => { this.error = err.message; this.loading = false; }
    });
  }

  private vide() {
    return { prenom: '', nom: '', email: '', tel: '', password: '' };
  }

  get visibles(): User[] {
    const t = this.texte.trim().toLowerCase();
    return this.users.filter(u => !t
      || `${u.prenom} ${u.nom}`.toLowerCase().includes(t) || u.email.toLowerCase().includes(t));
  }

  estMoi(u: User): boolean {
    return !!this.auth.currentUser && this.auth.currentUser.id === u.id;
  }

  // ---------- Formulaire ----------

  ouvrirCreation(): void {
    this.editingId = null;
    this.model = this.vide();
    this.roles = new Set<RoleName>(['CLIENT']);
    this.ouvrir();
  }

  ouvrirEdition(u: User): void {
    this.editingId = u.id;
    this.model = { prenom: u.prenom, nom: u.nom, email: u.email, tel: u.tel || '', password: '' };
    this.roles = new Set<RoleName>(u.roles);
    this.ouvrir();
  }

  private ouvrir(): void {
    this.formOuvert = true;
    this.submitted = false;
    this.formError = '';
    this.error = '';
    this.info = '';
    setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }), 0);
  }

  fermer(): void {
    this.formOuvert = false;
  }

  toggleRole(role: RoleName): void {
    if (this.roles.has(role)) { this.roles.delete(role); } else { this.roles.add(role); }
  }

  get emailInvalide(): boolean {
    return this.model.email.length > 0 && !EMAIL_RE.test(this.model.email.trim());
  }

  get motDePasseInvalide(): boolean {
    const p = this.model.password;
    if (this.editingId === null) { return p.length < 8; }   // création : obligatoire
    return p.length > 0 && p.length < 8;                    // modification : facultatif
  }

  enregistrer(valid: boolean | null): void {
    this.submitted = true;
    if (!valid || this.emailInvalide || this.motDePasseInvalide || this.roles.size === 0 || this.saving) { return; }

    const request: UserRequest = {
      email: this.model.email.trim().toLowerCase(),
      tel: this.model.tel.trim() || null,
      nom: this.model.nom.trim(),
      prenom: this.model.prenom.trim(),
      roles: Array.from(this.roles)
    };
    if (this.model.password) { request.password = this.model.password; }

    const creation = this.editingId === null;
    this.saving = true;
    this.formError = '';

    const appel = creation ? this.api.create(request) : this.api.update(this.editingId as number, request);
    appel.subscribe({
      next: u => {
        this.users = creation ? [...this.users, u] : this.users.map(x => x.id === u.id ? u : x);
        this.info = creation ? `Le compte de ${u.prenom} ${u.nom} a été créé.` : `Le compte de ${u.prenom} ${u.nom} a été modifié.`;
        this.saving = false;
        this.formOuvert = false;
      },
      error: (err: Error) => { this.formError = err.message; this.saving = false; }
    });
  }

  // ---------- Suppression ----------

  demanderSuppression(id: number): void {
    this.aSupprimer = id;
    this.error = '';
    this.info = '';
  }

  supprimer(u: User): void {
    this.occupe = u.id;
    this.api.delete(u.id).subscribe({
      next: () => {
        this.users = this.users.filter(x => x.id !== u.id);
        this.info = `Le compte de ${u.prenom} ${u.nom} a été supprimé.`;
        this.aSupprimer = null;
        this.occupe = null;
      },
      error: (err: Error) => { this.error = err.message; this.aSupprimer = null; this.occupe = null; }
    });
  }

  trackById(_: number, u: User): number {
    return u.id;
  }
}