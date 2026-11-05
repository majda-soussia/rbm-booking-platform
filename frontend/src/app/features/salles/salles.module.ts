import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { SallesListComponent } from './salles-list.component';

@NgModule({
  declarations: [SallesListComponent],
  imports: [
    CommonModule,
    FormsModule,
    RouterModule.forChild([{ path: '', component: SallesListComponent }])
  ]
})
export class SallesModule {}