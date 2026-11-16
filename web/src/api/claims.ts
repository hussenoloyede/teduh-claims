// Claim statuses and types come from the generated API types (schema.d.ts), so they always match
// ClaimStatus.java and ClaimType.java. The API sends the enum name (SUBMITTED); the screens show the label.
// If Java gains a status, TypeScript stops the build until it has a label here.

import { useQuery } from '@tanstack/react-query';
import { api, unwrap } from './client';
import type { components } from './schema';

export type ClaimSummary = components['schemas']['ClaimSummary'];
export type ClaimDetails = components['schemas']['ClaimDetails'];
export type TimelineEntry = components['schemas']['TimelineEntry'];
export type PolicyFacts = components['schemas']['PolicyFacts'];
export type ClaimStatus = ClaimSummary['status'];
export type ClaimType = ClaimSummary['claimType'];

export const statusLabel: Record<ClaimStatus, string> = {
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under review',
  MORE_INFO_NEEDED: 'More info needed',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
};

/** Every status, in the order a claim moves through them. Taken from the labels, so none can be missed. */
export const claimStatuses = Object.keys(statusLabel) as ClaimStatus[];

export const claimTypeLabel: Record<ClaimType, string> = {
  FLIGHT_DELAY: 'Flight delay',
  BAGGAGE_DELAYED: 'Baggage delayed',
  LOST_OR_DAMAGED_BAGGAGE: 'Lost or damaged baggage',
  TRIP_CANCELLATION: 'Trip cancellation',
};

/** Approved and rejected are final; every other status means the claim is still in progress. */
export function isDecided(status: ClaimStatus): boolean {
  return status === 'APPROVED' || status === 'REJECTED';
}

export const myClaimsQueryKey = ['claims', 'mine'] as const;

/** The signed-in customer's claims, newest first (the API sorts them). */
export function useMyClaims() {
  return useQuery({ queryKey: myClaimsQueryKey, queryFn: () => unwrap(api.GET('/api/claims')) });
}

export const claimQueryKey = (id: string) => ['claims', 'one', id] as const;

/** One claim with its documents and timeline. Someone else's claim answers 404, like one that doesn't exist. */
export function useClaim(id: string) {
  return useQuery({
    queryKey: claimQueryKey(id),
    queryFn: () => unwrap(api.GET('/api/claims/{id}', { params: { path: { id } } })),
  });
}

/**
 * Asks the API for a 15-minute link to one document, then follows it. The link answers with
 * Content-Disposition: attachment, so the browser downloads the file and the page stays where it is.
 */
export async function downloadDocument(claimId: string, documentId: string): Promise<void> {
  const link = await unwrap(
    api.GET('/api/claims/{id}/documents/{documentId}/link', { params: { path: { id: claimId, documentId } } }),
  );
  window.location.assign(link.url);
}

/** The agent queue's tabs, named as the API names them (QueueTab.java). */
export type QueueTab = 'submitted' | 'in-review-by-me' | 'waiting-on-customer' | 'decided';

export const queuesQueryKey = ['claims', 'queue'] as const;

/**
 * One tab of the agent queue, oldest first (the API sorts them). Asked for again every 30 seconds, so a
 * claim another agent picks up leaves the list without a reload.
 */
export function useQueue(tab: QueueTab) {
  return useQuery({
    queryKey: [...queuesQueryKey, tab],
    queryFn: () => unwrap(api.GET('/api/claims', { params: { query: { tab } } })),
    refetchInterval: 30_000,
  });
}
