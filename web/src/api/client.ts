// The one way the React app talks to the API.
//
// openapi-fetch is a thin layer over the browser's fetch that reads the generated types in schema.d.ts,
// so the address, the path and query parameters, the body and the answer are all checked by TypeScript.
// If an endpoint changes in Java, web/openapi.json changes, the types change, and the build fails here
// rather than on the live site.

import createClient, { type Middleware } from 'openapi-fetch';
import { noConnection, toApiError } from './problem';
import type { paths } from './schema';

/** Methods that change something. Only these need the CSRF token. */
const CHANGING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

/** Reads one cookie from a document.cookie string ("a=1; XSRF-TOKEN=abc"). */
export function readCookie(cookies: string, name: string): string | undefined {
  for (const part of cookies.split(';')) {
    const [key, ...value] = part.trim().split('=');
    if (key === name) {
      return decodeURIComponent(value.join('='));
    }
  }
  return undefined;
}

/**
 * Asks the server for a fresh XSRF-TOKEN cookie. Spring Security clears the token when someone signs in
 * or out, so this runs whenever a change is about to be sent and the cookie isn't there.
 */
export async function refreshCsrfToken(): Promise<void> {
  const response = await fetch('/api/auth/csrf', { credentials: 'same-origin' });
  if (!response.ok) {
    throw toApiError(response.status, await response.json().catch(() => undefined));
  }
}

/**
 * The CSRF token for a change, fetching one first if the cookie is missing. Spring Security compares the
 * X-XSRF-TOKEN header with the cookie. Another site can't read Teduh's cookie, so it can't send the header,
 * and a forged request from it is refused.
 */
export async function csrfToken(): Promise<string | undefined> {
  let token = readCookie(document.cookie, 'XSRF-TOKEN');
  if (!token) {
    await refreshCsrfToken();
    token = readCookie(document.cookie, 'XSRF-TOKEN');
  }
  return token;
}

const csrf: Middleware = {
  async onRequest({ request }) {
    if (CHANGING_METHODS.has(request.method)) {
      const token = await csrfToken();
      if (token) {
        request.headers.set('X-XSRF-TOKEN', token);
      }
    }
    return request;
  },
};

/**
 * The typed client: api.GET('/api/claims/{id}', { params: { path: { id } } }). Addresses are relative,
 * because the app and the API are on the same site (Vite forwards /api to Spring Boot in development).
 * The session cookie travels with every request on its own; the app never sees it.
 */
export const api = createClient<paths>({ credentials: 'same-origin' });
api.use(csrf);

type ApiResult<T> = { data?: T; error?: unknown; response: Response };

/**
 * Waits for a call and returns its data, or throws an ApiError. TanStack Query expects a failed request
 * to throw, so every query and mutation goes through this:
 *   queryFn: () => unwrap(api.GET('/api/auth/me'))
 */
export async function unwrap<T>(call: Promise<ApiResult<T>>): Promise<T> {
  let result: ApiResult<T>;
  try {
    result = await call;
  } catch {
    throw noConnection();
  }
  if (!result.response.ok) {
    throw toApiError(result.response.status, result.error);
  }
  return result.data as T;
}
