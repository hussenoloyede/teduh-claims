// Turning the server's error answers into one kind of error the screens can show.
//
// Teduh's API answers every error in the problem-details shape (RFC 9457), from the controllers and from
// Spring Security alike: { "title": "Not signed in", "status": 401, "detail": "Please sign in." }.
// The detail is written for people, so the screens can show it as it is.

export class ApiError extends Error {
  /** The HTTP status, or 0 when the server couldn't be reached at all. */
  readonly status: number;
  readonly title: string;

  constructor(status: number, title: string, detail: string) {
    super(detail);
    this.name = 'ApiError';
    this.status = status;
    this.title = title;
  }
}

type Problem = { title?: unknown; detail?: unknown };

function isProblem(body: unknown): body is Problem {
  return typeof body === 'object' && body !== null && ('title' in body || 'detail' in body);
}

/** Builds an ApiError from a failed answer, with a plain message when the body isn't problem details. */
export function toApiError(status: number, body: unknown): ApiError {
  if (isProblem(body)) {
    const title = typeof body.title === 'string' ? body.title : 'Something went wrong';
    const detail = typeof body.detail === 'string' ? body.detail : title;
    return new ApiError(status, title, detail);
  }
  if (status >= 500) {
    return new ApiError(status, 'Something went wrong', "Teduh couldn't answer just now. Please try again.");
  }
  return new ApiError(status, 'Something went wrong', `The request failed (status ${status}).`);
}

/** For a fetch that never got an answer: no connection, or the server is down. */
export const noConnection = (): ApiError =>
  new ApiError(0, 'No connection', "Couldn't reach Teduh. Check your connection and try again.");

/**
 * Whether asking again could help. A 4xx answer (not signed in, not allowed, a broken rule) won't change,
 * so it isn't retried. No connection or a 5xx might: the app may be starting up after scaling to zero.
 */
export function isWorthRetrying(error: unknown): boolean {
  if (!(error instanceof ApiError)) {
    return true;
  }
  return error.status === 0 || error.status >= 500;
}
