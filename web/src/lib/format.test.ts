import { describe, expect, it } from 'vitest';
import { formatDate, formatDateTime, formatFileSize, formatMoney, formatPlanMoney, formatWaiting, todayInMalaysia } from './format';

describe('formatMoney', () => {
  it('writes RM and two decimals', () => {
    expect(formatMoney(450)).toBe('RM 450.00');
  });

  it('groups thousands', () => {
    expect(formatMoney(2100)).toBe('RM 2,100.00');
  });

  it('keeps sen', () => {
    expect(formatMoney(0.1)).toBe('RM 0.10');
  });
});

describe('formatDate', () => {
  it('uses Malaysian time, so late evening UTC is already the next day', () => {
    expect(formatDate('2026-11-06T17:30:00Z')).toBe('7 Nov 2026');
  });
});

describe('formatDateTime', () => {
  it('writes a 12-hour time in Malaysian time', () => {
    expect(formatDateTime('2026-11-07T13:14:00Z')).toBe('7 Nov 2026, 9:14 pm');
  });
});

describe('formatFileSize', () => {
  it.each([
    [512, '512 B'],
    [317440, '310 KB'],
    [1258291, '1.2 MB'],
    [10485760, '10 MB'],
  ])('%i bytes is %s', (bytes, text) => {
    expect(formatFileSize(bytes)).toBe(text);
  });
});

describe('todayInMalaysia', () => {
  it('is already the next day in Malaysia late in the UTC evening', () => {
    expect(todayInMalaysia(new Date('2026-11-15T17:30:00Z'))).toBe('2026-11-16');
  });

  it('matches the UTC date earlier in the day', () => {
    expect(todayInMalaysia(new Date('2026-11-15T03:00:00Z'))).toBe('2026-11-15');
  });
});

describe('formatWaiting', () => {
  const now = new Date('2026-11-16T12:00:00Z');

  it('says less than an hour for a brand new claim', () => {
    expect(formatWaiting('2026-11-16T11:30:00Z', now)).toBe('less than an hour');
  });

  it('counts whole hours under a day', () => {
    expect(formatWaiting('2026-11-16T11:00:00Z', now)).toBe('1 hour');
    expect(formatWaiting('2026-11-16T07:00:00Z', now)).toBe('5 hours');
  });

  it('counts whole days from 24 hours on', () => {
    expect(formatWaiting('2026-11-15T12:00:00Z', now)).toBe('1 day');
    expect(formatWaiting('2026-11-13T02:00:00Z', now)).toBe('3 days');
  });
});

describe('formatPlanMoney', () => {
  it('leaves out the sen on whole ringgit', () => {
    expect(formatPlanMoney(49)).toBe('RM 49');
    expect(formatPlanMoney(10000)).toBe('RM 10,000');
  });

  it('keeps the sen when there are any', () => {
    expect(formatPlanMoney(49.5)).toBe('RM 49.50');
  });
});
