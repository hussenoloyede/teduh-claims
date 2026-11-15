// TanStack Query keeps what the server sent (claims, notifications) in one cache, tracks loading and
// error states, and asks again when the tab regains focus. Local screen state stays in useState.

import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query';
import { meQueryKey } from './api/auth';
import { ApiError, isWorthRetrying } from './api/problem';

/**
 * A 401 from any call means the session has ended (30 idle minutes, or the server restarted). Marking
 * "nobody is signed in" makes the page guards send the person to the sign-in page, which brings them back
 * here afterwards.
 */
function signedOutOn401(error: unknown) {
  if (error instanceof ApiError && error.status === 401) {
    queryClient.setQueryData(meQueryKey, null);
  }
}

export const queryClient = new QueryClient({
  queryCache: new QueryCache({ onError: signedOutOn401 }),
  mutationCache: new MutationCache({ onError: signedOutOn401 }),
  defaultOptions: {
    queries: {
      // At most two more tries, and only when asking again could help (see isWorthRetrying).
      retry: (failureCount, error) => failureCount < 2 && isWorthRetrying(error),
    },
  },
});
