import { describe, expect, it } from 'vitest';
import { formatDate, formatDateTime, formatFileSize, formatMoney } from './format';

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
