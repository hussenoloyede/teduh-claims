// Claim statuses and types come from the generated API types (schema.d.ts), so they always match
// ClaimStatus.java and ClaimType.java. The API sends the enum name (SUBMITTED); the screens show the label.
// If Java gains a status, TypeScript stops the build until it has a label here.

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
