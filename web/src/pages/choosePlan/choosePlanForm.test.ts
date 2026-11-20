import { describe, expect, it } from 'vitest';
import { addDays, checkTripDates, formatDays, formatTrip, latestStart, tripDays } from './choosePlanForm';

const today = '2026-11-21';

describe('trip dates', () => {
  it('counts both the first and the last day', () => {
    expect(tripDays('2026-12-01', '2026-12-14')).toBe(14);
    expect(tripDays('2026-12-01', '2026-12-01')).toBe(1);
    expect(tripDays('2026-12-28', '2027-01-03')).toBe(7);
  });

  it('adds days across months and years', () => {
    expect(addDays('2026-11-21', 6)).toBe('2026-11-27');
    expect(addDays('2026-12-30', 3)).toBe('2027-01-02');
  });

  it('allows a trip that starts today', () => {
    expect(checkTripDates(today, addDays(today, 6), today)).toEqual({});
  });

  it('allows exactly 90 days and refuses 91', () => {
    expect(checkTripDates(today, addDays(today, 89), today)).toEqual({});
    expect(checkTripDates(today, addDays(today, 90), today).end).toBe('One policy covers at most 90 days.');
  });

  it('refuses a start in the past or more than a year away', () => {
    expect(checkTripDates('2026-11-20', '2026-11-25', today).start).toBe("Cover can't start before today.");
    expect(latestStart(today)).toBe('2027-11-21');
    expect(checkTripDates('2027-11-22', '2027-11-25', today).start).toBe('Cover must start within a year from today.');
  });

  it('refuses a trip that ends before it starts', () => {
    expect(checkTripDates('2026-12-14', '2026-12-01', today).end).toBe("The trip can't end before it starts.");
  });

  it('asks for missing dates', () => {
    expect(checkTripDates('', '', today)).toEqual({
      start: 'Enter the day you leave.',
      end: 'Enter the day you get home.',
    });
  });
});

describe('formatting a trip', () => {
  it('names the year once when the trip stays in one year', () => {
    expect(formatTrip('2026-12-01', '2026-12-14')).toBe('1 Dec to 14 Dec 2026');
  });

  it('names both years when the trip crosses into a new one', () => {
    expect(formatTrip('2026-12-28', '2027-01-03')).toBe('28 Dec 2026 to 3 Jan 2027');
  });

  it('says day or days', () => {
    expect(formatDays(1)).toBe('1 day');
    expect(formatDays(14)).toBe('14 days');
  });
});
