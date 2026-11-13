// Claim statuses and types, named exactly as in the backend's ClaimStatus.java
// and ClaimType.java. The API sends the enum name (SUBMITTED); the screens
// show the label. If one changes in Java, change it here too.

export const claimStatuses = [
  'SUBMITTED',
  'UNDER_REVIEW',
  'MORE_INFO_NEEDED',
  'APPROVED',
  'REJECTED',
] as const;

export type ClaimStatus = (typeof claimStatuses)[number];

export const statusLabel: Record<ClaimStatus, string> = {
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under review',
  MORE_INFO_NEEDED: 'More info needed',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
};

export type ClaimType =
  | 'FLIGHT_DELAY'
  | 'BAGGAGE_DELAYED'
  | 'LOST_OR_DAMAGED_BAGGAGE'
  | 'TRIP_CANCELLATION';

export const claimTypeLabel: Record<ClaimType, string> = {
  FLIGHT_DELAY: 'Flight delay',
  BAGGAGE_DELAYED: 'Baggage delayed',
  LOST_OR_DAMAGED_BAGGAGE: 'Lost or damaged baggage',
  TRIP_CANCELLATION: 'Trip cancellation',
};
