import { describe, expect, it } from 'vitest';
import { greetingName, homeFor, landingAfterSignIn } from './auth';

describe('homeFor', () => {
  it('starts customers at home and agents at the queue', () => {
    expect(homeFor('CUSTOMER')).toBe('/');
    expect(homeFor('AGENT')).toBe('/agent/queue');
  });
});

describe('landingAfterSignIn', () => {
  it('goes back to the page that asked for a sign-in', () => {
    expect(landingAfterSignIn('CUSTOMER', '/claims/123')).toBe('/claims/123');
    expect(landingAfterSignIn('AGENT', '/agent/history')).toBe('/agent/history');
  });

  it('goes to the start page when there is no such page', () => {
    expect(landingAfterSignIn('CUSTOMER')).toBe('/');
    expect(landingAfterSignIn('AGENT', '/sign-in')).toBe('/agent/queue');
  });

  it("never sends someone into the other role's pages", () => {
    expect(landingAfterSignIn('CUSTOMER', '/agent/queue')).toBe('/');
    expect(landingAfterSignIn('AGENT', '/claims/123')).toBe('/agent/queue');
    expect(landingAfterSignIn('AGENT', '/agentless')).toBe('/agent/queue');
  });
});

describe('greetingName', () => {
  it('uses the first name', () => {
    expect(greetingName('Aisyah Rahman')).toBe('Aisyah');
  });

  it('keeps a new demo customer as "Demo traveller"', () => {
    expect(greetingName('Demo traveller')).toBe('Demo traveller');
  });
});
