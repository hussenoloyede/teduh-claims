import { describe, expect, it } from 'vitest';
import { readCookie, unwrap } from './client';
import { ApiError } from './problem';

describe('readCookie', () => {
  it('finds one cookie among several', () => {
    expect(readCookie('theme=dark; XSRF-TOKEN=abc-123; other=1', 'XSRF-TOKEN')).toBe('abc-123');
  });

  it('keeps an equals sign inside the value and decodes it', () => {
    expect(readCookie('XSRF-TOKEN=a%3Db=c', 'XSRF-TOKEN')).toBe('a=b=c');
  });

  it('is undefined when the cookie is missing', () => {
    expect(readCookie('', 'XSRF-TOKEN')).toBeUndefined();
    expect(readCookie('XSRF-TOKEN-OLD=x', 'XSRF-TOKEN')).toBeUndefined();
  });
});

describe('unwrap', () => {
  it('returns the data of a successful answer', async () => {
    const call = Promise.resolve({ data: { reference: 'CLM-2026-000017' }, response: new Response(null, { status: 200 }) });
    await expect(unwrap(call)).resolves.toEqual({ reference: 'CLM-2026-000017' });
  });

  it('throws an ApiError with the server message for a failed answer', async () => {
    const call = Promise.resolve({
      error: { title: 'Rule broken', status: 422, detail: 'A reason is needed when approving less than claimed.' },
      response: new Response(null, { status: 422 }),
    });
    await expect(unwrap(call)).rejects.toMatchObject({
      status: 422,
      message: 'A reason is needed when approving less than claimed.',
    });
  });

  it('throws a no-connection ApiError when fetch itself fails', async () => {
    const error = await unwrap(Promise.reject(new TypeError('Failed to fetch'))).catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(0);
  });
});
