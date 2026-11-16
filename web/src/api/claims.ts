// Claim statuses and types come from the generated API types (schema.d.ts), so they always match
// ClaimStatus.java and ClaimType.java. The API sends the enum name (SUBMITTED); the screens show the label.
// If Java gains a status, TypeScript stops the build until it has a label here.

import { useQuery } from '@tanstack/react-query';
import { api, unwrap } from './client';
import type { components } from './schema';

export type ClaimSummary = components['schemas']['ClaimSummary'];
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
