// Claim statuses, in the same order as ClaimStatus.java in the backend.
// The API sends the enum name (SUBMITTED); the screens show the label.
// If a status is added or renamed in Java, change it here too.

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
