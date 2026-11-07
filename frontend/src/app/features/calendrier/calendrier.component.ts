import { Component, OnInit, ViewChild } from '@angular/core';
import { CalendarOptions, EventClickArg, EventInput, FullCalendarComponent } from '@fullcalendar/angular';
import frLocale from '@fullcalendar/core/locales/fr';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import timeGridPlugin from '@fullcalendar/timegrid';
import { CalendrierEvent } from '../../core/models/calendrier.model';
import { Salle } from '../../core/models/salle.model';
import { StatutReservation } from '../../core/models/reservation.model';
import { AuthService } from '../../core/services/auth.service';
import { ReservationService } from '../../core/services/reservation.service';
import { SalleService } from '../../core/services/salle.service';
import { dateToLocalIso } from '../../core/utils/date.util';

const COULEURS: Record<StatutReservation, string> = {
  EN_ATTENTE: '#fb6340',
  CONFIRMEE: '#2dce89',
  REFUSEE: '#f5365c',
  ANNULEE: '#8898aa'
};
const COULEUR_OCCUPE = '#5e72e4';   // créneau d'un autre client (masqué)

@Component({
  selector: 'app-calendrier',
  templateUrl: './calendrier.component.html',
  styleUrls: ['./calendrier.component.scss']
})
export class CalendrierComponent implements OnInit {

  @ViewChild('calendar') calendar!: FullCalendarComponent;

  readonly legende = [
    { label: 'En attente', couleur: COULEURS.EN_ATTENTE },
    { label: 'Confirmée', couleur: COULEURS.CONFIRMEE },
    { label: 'Refusée', couleur: COULEURS.REFUSEE },
    { label: 'Annulée', couleur: COULEURS.ANNULEE }
  ];
  readonly legendeOccupe = COULEUR_OCCUPE;

  salles: Salle[] = [];
  salleId: number | null = null;
  selection: CalendrierEvent | null = null;
  error = '';

  options: CalendarOptions = {
    plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
    initialView: 'dayGridMonth',
    locale: frLocale,
    firstDay: 1,
    height: 'auto',
    nowIndicator: true,
    allDaySlot: false,
    slotMinTime: '07:00:00',
    slotMaxTime: '22:00:00',
    slotDuration: '00:30:00',
    dayMaxEvents: 3,
    eventTimeFormat: { hour: '2-digit', minute: '2-digit', hour12: false },
    headerToolbar: {
      left: 'prev,next today',
      center: 'title',
      right: 'dayGridMonth,timeGridWeek,timeGridDay'
    },
    events: (info, success, failure) => {
      this.api.calendrier(dateToLocalIso(info.start), dateToLocalIso(info.end)).subscribe({
        next: list => {
          this.error = '';
          success(this.versEvenements(list));
        },
        error: (err: Error) => {
          this.error = err.message;
          failure(err);
        }
      });
    },
    eventClick: (arg: EventClickArg) => {
      this.selection = arg.event.extendedProps['source'] as CalendrierEvent;
    }
  };

  constructor(
    private api: ReservationService,
    private sallesApi: SalleService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    this.sallesApi.getAll().subscribe({
      next: list => this.salles = list.filter(s => s.active || this.auth.isAdmin),
      error: () => { /* le filtre par salle est facultatif */ }
    });
  }

  /** Les couleurs viennent du statut ; le backend décide de ce qui est visible. */
  private versEvenements(list: CalendrierEvent[]): EventInput[] {
    return list
      .filter(e => this.salleId === null || e.salleId === this.salleId)
      .map((e, i) => {
        const masque = e.id === null;
        const couleur = masque ? COULEUR_OCCUPE : COULEURS[e.statut];
        return {
          id: masque ? `occ-${i}` : String(e.id),
          title: masque ? `${e.salleNom} · Occupé` : `${e.salleNom} · ${e.titre}`,
          start: e.dateDebut,
          end: e.dateFin,
          backgroundColor: couleur,
          borderColor: couleur,
          classNames: e.statut === 'ANNULEE' || e.statut === 'REFUSEE' ? ['barre'] : [],
          extendedProps: { source: e }
        };
      });
  }

  changerSalle(): void {
    this.selection = null;
    this.calendar.getApi().refetchEvents();
  }

  libelle(statut: StatutReservation): string {
    return { EN_ATTENTE: 'En attente', CONFIRMEE: 'Confirmée', REFUSEE: 'Refusée', ANNULEE: 'Annulée' }[statut];
  }
}