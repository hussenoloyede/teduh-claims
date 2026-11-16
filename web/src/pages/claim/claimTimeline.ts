// Turns a claim's timeline (one line per status change) into the words the Timeline component shows.
// Shared by the customer's claim page and, later, the agent's.

import type { ClaimDetails, ClaimStatus, TimelineEntry } from '../../api/claims';
import type { TimelineItem } from '../../components/Timeline/Timeline';

/** "you" for the person looking, otherwise the person's name. */
function who(entry: TimelineEntry, viewerId: string): string {
  return entry.actorId === viewerId ? 'you' : entry.actorName;
}

export function describe(entry: TimelineEntry, viewerId: string): string {
  const by = who(entry, viewerId);
  switch (entry.toStatus) {
    case 'SUBMITTED':
      return `Submitted by ${by}`;
    case 'UNDER_REVIEW':
      return entry.fromStatus === 'MORE_INFO_NEEDED'
        ? `${by === 'you' ? 'You' : by} sent more information`
        : `Review started by ${by}`;
    case 'MORE_INFO_NEEDED':
      return `More information requested by ${by}`;
    case 'APPROVED':
      return `Approved by ${by}`;
    case 'REJECTED':
      return `Rejected by ${by}`;
  }
}

/** The steps still to come, so the customer can see how far along the claim is. Final claims have none. */
function upcoming(status: ClaimStatus): string[] {
  switch (status) {
    case 'SUBMITTED':
      return ['Review', 'Decision'];
    case 'UNDER_REVIEW':
    case 'MORE_INFO_NEEDED':
      return ['Decision'];
    default:
      return [];
  }
}

export function timelineItems(claim: Pick<ClaimDetails, 'status' | 'timeline'>, viewerId: string): TimelineItem[] {
  const done = claim.timeline.map((entry, index) => ({
    id: `step-${index}`,
    title: describe(entry, viewerId),
    time: entry.occurredAt,
    detail: entry.message ?? undefined,
  }));
  const next = upcoming(claim.status).map((title) => ({ id: `next-${title}`, title, upcoming: true }));
  return [...done, ...next];
}

/** The latest request for more information: what the agent asked for, and who asked. */
export function latestRequest(claim: Pick<ClaimDetails, 'timeline'>): TimelineEntry | undefined {
  return claim.timeline.findLast((entry) => entry.toStatus === 'MORE_INFO_NEEDED');
}
