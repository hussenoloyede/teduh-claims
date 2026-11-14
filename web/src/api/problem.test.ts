import { describe, expect, it } from 'vitest';
import { ApiError, isWorthRetrying, noConnection, toApiError } from './problem';

describe('toApiError', () => {
  it('keeps the title and detail from a problem-details body', () => {
    const error = toApiError(409, {
      type: 'about:blank',
      title: 'Claim changed',
      status: 409,
      detail: 'This claim changed while you were looking at it. Reload to see the latest.',
    });
    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(409);
    expect(error.title).toBe('Claim changed');
    expect(error.message).toBe('This claim changed while you were looking at it. Reload to see the latest.');
  });

  it('uses the title as the message when there is no detail', () => {
    expect(toApiError(403, { title: 'Not allowed' }).message).toBe('Not allowed');
  });

  it('gives a plain message when a server error has no problem details', () => {
    const error = toApiError(502, '<html>Bad gateway</html>');
    expect(error.status).toBe(502);
    expect(error.message).toBe("Teduh couldn't answer just now. Please try again.");
  });

  it('names the status when a client error has no problem details', () => {
    expect(toApiError(404, undefined).message).toBe('The request failed (status 404).');
  });
});

describe('isWorthRetrying', () => {
  it('retries when the server could not be reached or failed', () => {
    expect(isWorthRetrying(noConnection())).toBe(true);
    expect(isWorthRetrying(toApiError(503, undefined))).toBe(true);
  });

  it('does not retry answers that would not change', () => {
    for (const status of [400, 401, 403, 404, 409, 422]) {
      expect(isWorthRetrying(toApiError(status, { title: 'No' }))).toBe(false);
    }
  });
});
