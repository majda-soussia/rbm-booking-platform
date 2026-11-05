import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { DisponibilitesComponent } from './disponibilites.component';
import { ReservationFormComponent } from './reservation-form.component';

const routes: Routes = [
  { path: '', redirectTo: 'disponibilites', pathMatch: 'full' },
  { path: 'disponibilites', component: DisponibilitesComponent },
  { path: 'nouvelle', component: ReservationFormComponent }
];

@NgModule({
  declarations: [DisponibilitesComponent, ReservationFormComponent],
  imports: [CommonModule, FormsModule, RouterModule.forChild(routes)]
})
export class ReservationsModule {}