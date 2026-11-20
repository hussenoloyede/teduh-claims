// The rules for the trip dates on C17, the same ones the server checks (PolicySales.java). Checking them
// here too means the customer hears about a mistake before pressing Continue, not after the last step.
//
// Dates stay as "2026-12-01" strings, the form both the date inputs and the API use. They sort the same way
// as the days they stand for, so comparing the strings compares the dates.

/** The longest trip one policy covers, counting the first and the last day. */
export const MAX_TRIP_DAYS = 90;

/** "2026-12-01" plus 13 days → "2026-12-14". Done in UTC so a daylight-saving change can't shift the day. */
export function addDays(isoDate: string, days: number): string {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day + days)).toISOString().slice(0, 10);
}

/** How many days of cover a trip gets. Both ends count: 1 Dec to 14 Dec is 14 days. */
export function tripDays(start: string, end: string): number {
  const toDay = (iso: string) => Date.parse(`${iso}T00:00:00Z`) / 86_400_000;
  return toDay(end) - toDay(start) + 1;
}

/** The latest day a trip can start: a year from today. */
export function latestStart(today: string): string {
  const [year, month, day] = today.split('-').map(Number);
  return new Date(Date.UTC(year + 1, month - 1, day)).toISOString().slice(0, 10);
}

export type TripDateErrors = { start?: string; end?: string };

/** What is wrong with the trip dates, if anything. Each message says how to fix it. */
export function checkTripDates(start: string, end: string, today: string): TripDateErrors {
  const errors: TripDateErrors = {};
  if (!start) {
    errors.start = 'Enter the day you leave.';
  } else if (start < today) {
    errors.start = "Cover can't start before today.";
  } else if (start > latestStart(today)) {
    errors.start = 'Cover must start within a year from today.';
  }
  if (!end) {
    errors.end = 'Enter the day you get home.';
  } else if (start && end < start) {
    errors.end = "The trip can't end before it starts.";
  } else if (start && tripDays(start, end) > MAX_TRIP_DAYS) {
    errors.end = `One policy covers at most ${MAX_TRIP_DAYS} days.`;
  }
  return errors;
}

const dayMonth = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', timeZone: 'UTC' });
const full = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' });

/** "1 Dec to 14 Dec 2026", or "28 Dec 2026 to 3 Jan 2027" when the trip crosses into a new year. */
export function formatTrip(start: string, end: string): string {
  const startDate = new Date(`${start}T00:00:00Z`);
  const endDate = new Date(`${end}T00:00:00Z`);
  const sameYear = start.slice(0, 4) === end.slice(0, 4);
  return `${(sameYear ? dayMonth : full).format(startDate)} to ${full.format(endDate)}`;
}

/** "1 day", "14 days". */
export function formatDays(days: number): string {
  return days === 1 ? '1 day' : `${days} days`;
}
