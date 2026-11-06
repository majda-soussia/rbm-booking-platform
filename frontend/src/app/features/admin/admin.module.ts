import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { AdminReservationsComponent } from './admin-reservations.component';

const routes: Routes = [
  { path: '', redirectTo: 'reservations', pathMatch: 'full' },
  { path: 'reservations', component: AdminReservationsComponent }
];

@NgModule({
  declarations: [AdminReservationsComponent],
  imports: [CommonModule, FormsModule, RouterModule.forChild(routes)]
})
export class AdminModule {}