import { Routes } from '@angular/router';

import { DashboardComponent } from '../../pages/dashboard/dashboard.component';
import { IconsComponent } from '../../pages/icons/icons.component';
import { MapsComponent } from '../../pages/maps/maps.component';
import { UserProfileComponent } from '../../pages/user-profile/user-profile.component';
import { TablesComponent } from '../../pages/tables/tables.component';
import { RoleGuard } from '../../core/guards/role.guard';

export const AdminLayoutRoutes: Routes = [
    { path: 'dashboard',      component: DashboardComponent },
    { path: 'user-profile',   component: UserProfileComponent },
    { path: 'tables',         component: TablesComponent },
    { path: 'icons',          component: IconsComponent },
    { path: 'maps',           component: MapsComponent },
    { path: 'salles', loadChildren: () => import('../../features/salles/salles.module').then(m => m.SallesModule) },
    { path: 'reservations', loadChildren: () => import('../../features/reservations/reservations.module').then(m => m.ReservationsModule) },
    {
  path: 'admin',
  canActivate: [RoleGuard],
  data: { roles: ['ADMIN'] },
  loadChildren: () => import('../../features/admin/admin.module').then(m => m.AdminModule)
},
];
