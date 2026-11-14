// TanStack Query keeps what the server sent (claims, notifications) in one cache, tracks loading and
// error states, and asks again when the tab regains focus. Local screen state stays in useState.

import { QueryClient } from '@tanstack/react-query';
import { isWorthRetrying } from './api/problem';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // At most two more tries, and only when asking again could help (see isWorthRetrying).
      retry: (failureCount, error) => failureCount < 2 && isWorthRetrying(error),
    },
  },
});
