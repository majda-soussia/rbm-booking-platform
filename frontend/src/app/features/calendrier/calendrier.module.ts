import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FullCalendarModule } from '@fullcalendar/angular';
import { CalendrierComponent } from './calendrier.component';

@NgModule({
  declarations: [CalendrierComponent],
  imports: [
    CommonModule,
    FormsModule,
    FullCalendarModule,
    RouterModule.forChild([{ path: '', component: CalendrierComponent }])
  ]
})
export class CalendrierModule {}