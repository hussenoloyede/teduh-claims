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
