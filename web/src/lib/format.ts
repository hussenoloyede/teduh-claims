// How amounts, dates and file sizes are written on screen. The API sends
// amounts as numbers in ringgit and times as UTC instants
// ("2026-11-07T13:14:00Z"); Teduh always shows Malaysian time.

const timeZone = 'Asia/Kuala_Lumpur';

const money = new Intl.NumberFormat('en-MY', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/** 2100 → "RM 2,100.00" */
export function formatMoney(amount: number): string {
  return `RM ${money.format(amount)}`;
}

const wholeMoney = new Intl.NumberFormat('en-MY', { maximumFractionDigits: 0 });

/**
 * 1500 → "RM 1,500", for prices and plan limits, which are always whole ringgit. Anything with sen falls
 * back to formatMoney, so an amount is never rounded on screen.
 */
export function formatPlanMoney(amount: number): string {
  return Number.isInteger(amount) ? `RM ${wholeMoney.format(amount)}` : formatMoney(amount);
}

const date = new Intl.DateTimeFormat('en-GB', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  timeZone,
});

/** "2026-11-07T13:14:00Z" → "7 Nov 2026" */
export function formatDate(iso: string): string {
  return date.format(new Date(iso));
}

const dateTime = new Intl.DateTimeFormat('en-GB', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
  hour12: true,
  timeZone,
});

/** "2026-11-07T13:14:00Z" → "7 Nov 2026, 9:14 pm" */
export function formatDateTime(iso: string): string {
  // Newer browsers put a narrow no-break space before "pm"; use a plain one
  // so the text is the same everywhere.
  return dateTime.format(new Date(iso)).replace(/ /g, ' ');
}

/** 317440 → "310 KB", 1258291 → "1.2 MB", 10485760 → "10 MB" */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${Number((bytes / (1024 * 1024)).toFixed(1))} MB`;
}

const isoDay = new Intl.DateTimeFormat('en-CA', { year: 'numeric', month: '2-digit', day: '2-digit', timeZone });

/**
 * Today's date in Malaysia as "2026-11-16", the same form the API uses for dates such as a policy's cover
 * start. Late in the evening UTC it is already tomorrow in Kuala Lumpur, so this never uses the browser's
 * own time zone. (en-CA is the locale whose dates read year-month-day.)
 */
export function todayInMalaysia(now: Date = new Date()): string {
  return isoDay.format(now);
}

/**
 * How long something has waited, in the largest whole unit: "less than an hour", "5 hours", "1 day",
 * "3 days". Used for how long a claim has been in the queue.
 */
export function formatWaiting(sinceIso: string, now: Date = new Date()): string {
  const hours = Math.floor((now.getTime() - new Date(sinceIso).getTime()) / 3_600_000);
  if (hours < 1) return 'less than an hour';
  if (hours < 24) return hours === 1 ? '1 hour' : `${hours} hours`;
  const days = Math.floor(hours / 24);
  return days === 1 ? '1 day' : `${days} days`;
}
