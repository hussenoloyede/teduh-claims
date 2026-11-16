import { useMutation, useQuery } from '@tanstack/react-query';
import { queryClient } from '../queryClient';
import { api, unwrap } from './client';

export const inboxQueryKey = ['notifications'] as const;

/**
 * The bell's number and the list under it, in one request. TanStack Query asks again every 30 seconds while
 * the page is open, and as soon as the tab regains focus, so a new notification shows up without a reload.
 * The bell and the notifications page share this one cache entry, so reading one updates the other.
 */
export function useInbox() {
  return useQuery({
    queryKey: inboxQueryKey,
    queryFn: () => unwrap(api.GET('/api/notifications')),
    refetchInterval: 30_000,
  });
}

const refreshInbox = () => queryClient.invalidateQueries({ queryKey: inboxQueryKey });

export function useMarkRead() {
  return useMutation({
    mutationFn: (id: string) => unwrap(api.POST('/api/notifications/{id}/read', { params: { path: { id } } })),
    onSettled: refreshInbox,
  });
}

export function useMarkAllRead() {
  return useMutation({
    mutationFn: () => unwrap(api.POST('/api/notifications/read-all')),
    onSettled: refreshInbox,
  });
}
