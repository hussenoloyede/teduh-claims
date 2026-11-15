import { useQuery } from '@tanstack/react-query';
import { api, unwrap } from './client';

export const inboxQueryKey = ['notifications'] as const;

/**
 * The bell's number and the list under it, in one request. TanStack Query asks again every 30 seconds while
 * the page is open, and as soon as the tab regains focus, so a new notification shows up without a reload.
 */
export function useInbox() {
  return useQuery({
    queryKey: inboxQueryKey,
    queryFn: () => unwrap(api.GET('/api/notifications')),
    refetchInterval: 30_000,
  });
}
