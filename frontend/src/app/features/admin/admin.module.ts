import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { AdminEquipementsComponent } from './admin-equipements.component';
import { AdminReservationsComponent } from './admin-reservations.component';
import { AdminSallesComponent } from './admin-salles.component';
import { AdminUtilisateursComponent } from './admin-utilisateurs.component';

const routes: Routes = [
  { path: '', redirectTo: 'reservations', pathMatch: 'full' },
  { path: 'reservations', component: AdminReservationsComponent },
  { path: 'salles', component: AdminSallesComponent },
  { path: 'equipements', component: AdminEquipementsComponent },
  { path: 'utilisateurs', component: AdminUtilisateursComponent }
];

@NgModule({
  declarations: [
    AdminReservationsComponent,
    AdminSallesComponent,
    AdminEquipementsComponent,
    AdminUtilisateursComponent
  ],
  imports: [CommonModule, FormsModule, RouterModule.forChild(routes)]
})
export class AdminModule {}