import { describe, expect, it } from 'vitest';
import { coverState, limitsOf } from './policies';

const cover = { coverStart: '2026-10-01', coverEnd: '2026-12-31' };

describe('coverState', () => {
  it('counts both ends as covered', () => {
    expect(coverState(cover, '2026-10-01')).toBe('active');
    expect(coverState(cover, '2026-12-31')).toBe('active');
  });

  it('knows a policy that has not started or has ended', () => {
    expect(coverState(cover, '2026-09-30')).toBe('upcoming');
    expect(coverState(cover, '2027-01-01')).toBe('ended');
  });
});

describe('limitsOf', () => {
  it('lists only the covered types, in a fixed order, with their labels', () => {
    const limits = { TRIP_CANCELLATION: 10000, FLIGHT_DELAY: 500 };
    expect(limitsOf({ limits })).toEqual([
      { type: 'FLIGHT_DELAY', label: 'Flight delay', limit: 500 },
      { type: 'TRIP_CANCELLATION', label: 'Trip cancellation', limit: 10000 },
    ]);
  });
});
