import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { DisponibilitesComponent } from './disponibilites.component';
import { ReservationFormComponent } from './reservation-form.component';
import { MesReservationsComponent } from './mes-reservations.component';

const routes: Routes = [
  { path: '', redirectTo: 'disponibilites', pathMatch: 'full' },
  { path: 'disponibilites', component: DisponibilitesComponent },
  { path: 'nouvelle', component: ReservationFormComponent },
  { path: 'mes', component: MesReservationsComponent }
];

@NgModule({
  declarations: [DisponibilitesComponent, ReservationFormComponent, MesReservationsComponent],
  imports: [CommonModule, FormsModule, RouterModule.forChild(routes)]
})
export class ReservationsModule {}