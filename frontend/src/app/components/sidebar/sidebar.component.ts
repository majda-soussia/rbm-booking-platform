import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { RoleName } from '../../core/models/user.model';
import { AuthService } from '../../core/services/auth.service';
declare interface RouteInfo {
  path: string;
  title: string;
  icon: string;
  class: string;
  roles?: RoleName[];   // si absent : visible par tous les connectés
}
export const ROUTES: RouteInfo[] = [
  { path: '/dashboard', title: 'Tableau de bord', icon: 'ni-tv-2 text-primary', class: '' },
  { path: '/user-profile', title: 'Mon profil', icon: 'ni-single-02 text-yellow', class: '' },
  { path: '/salles', title: 'Salles', icon: 'ni-building text-blue', class: '' },
  { path: '/reservations/disponibilites', title: 'Disponibilités', icon: 'ni-calendar-grid-58 text-orange', class: '' },
  { path: '/reservations/mes', title: 'Mes réservations', icon: 'ni-collection text-info', class: '' },
  // à venir : Salles, Disponibilités, Mes réservations, Calendrier  (CLIENT et ADMIN)
  // à venir : Toutes les réservations, Gestion des salles, Équipements, Utilisateurs  (ADMIN)
];

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent implements OnInit {
  public menuItems: any[];
  public isCollapsed = true;

  constructor(private router: Router, private auth: AuthService) {}

  ngOnInit() {
    this.menuItems = ROUTES.filter(item =>
      !item.roles || item.roles.some(role => this.auth.hasRole(role))
    );
    this.router.events.subscribe(() => {
      this.isCollapsed = true;
    });
  }
  logout(): void {
  this.auth.logout();
}
}
