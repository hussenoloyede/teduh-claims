import { describe, expect, it } from 'vitest';
import { agentNav, sectionOf } from './navigation';

describe('sectionOf', () => {
  it('finds the bottom tab for each customer page', () => {
    expect(sectionOf('/')).toBe('home');
    expect(sectionOf('/claims')).toBe('claims');
    expect(sectionOf('/claims/123')).toBe('claims');
    expect(sectionOf('/policies')).toBe('policies');
    expect(sectionOf('/profile')).toBe('profile');
  });

  it('puts choosing a plan under Policies and help under Profile', () => {
    expect(sectionOf('/plans')).toBe('policies');
    expect(sectionOf('/help')).toBe('profile');
  });

  it('marks no tab on pages outside the four', () => {
    expect(sectionOf('/notifications')).toBeUndefined();
  });
});

describe('agentNav', () => {
  it('marks only the current page', () => {
    const nav = agentNav('/agent/history');
    expect(nav.filter((item) => item.current).map((item) => item.label)).toEqual(['History']);
  });

  it('keeps the queue current inside a claim opened from it', () => {
    expect(agentNav('/agent/queue').find((item) => item.current)?.label).toBe('Claims queue');
  });
});
