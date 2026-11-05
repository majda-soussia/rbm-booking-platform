/** Date du jour au format AAAA-MM-JJ (heure locale), pour <input type="date">. */
export function todayStr(): string {
  const d = new Date();
  const mois = String(d.getMonth() + 1).padStart(2, '0');
  const jour = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mois}-${jour}`;
}

/** Créneaux par pas de 30 minutes : "07:00", "07:30", ... "22:00". */
export function timeSlots(fromHour: number = 7, toHour: number = 22): string[] {
  const slots: string[] = [];
  for (let h = fromHour; h <= toHour; h++) {
    slots.push(`${String(h).padStart(2, '0')}:00`);
    if (h < toHour) { slots.push(`${String(h).padStart(2, '0')}:30`); }
  }
  return slots;
}

/** Format attendu par le backend (LocalDateTime) : "2026-12-01T14:00:00". */
export function toLocalIso(date: string, time: string): string {
  return `${date}T${time}:00`;
}

/** Minutes entre deux heures "HH:mm". */
export function minutesBetween(debut: string, fin: string): number {
  const [h1, m1] = debut.split(':').map(Number);
  const [h2, m2] = fin.split(':').map(Number);
  return (h2 * 60 + m2) - (h1 * 60 + m1);
}

export function isPast(date: string, time: string): boolean {
  return new Date(`${date}T${time}:00`).getTime() < Date.now();
}