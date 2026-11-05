import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { RoleName } from '../models/user.model';
import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {

  constructor(private auth: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean | UrlTree {
    if (!this.auth.isLoggedIn) {
      return this.router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }
    const roles = (route.data['roles'] || []) as RoleName[];
    if (roles.length === 0 || roles.some(r => this.auth.hasRole(r))) {
      return true;
    }
    return this.router.createUrlTree(['/dashboard']);   // connecté mais pas autorisé
  }
}