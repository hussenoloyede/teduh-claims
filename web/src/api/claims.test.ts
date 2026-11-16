import { describe, expect, it } from 'vitest';
import { claimStatuses, isDecided } from './claims';

describe('isDecided', () => {
  it('treats approved and rejected as final and everything else as in progress', () => {
    expect(claimStatuses.filter(isDecided)).toEqual(['APPROVED', 'REJECTED']);
  });
});
