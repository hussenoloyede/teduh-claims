import { describe as group, expect, it } from 'vitest';
import type { TimelineEntry } from '../../api/claims';
import { describe, latestRequest, timelineItems } from './claimTimeline';

const aisyah = 'u-aisyah';
const farid = 'u-farid';

const line = (from: TimelineEntry['fromStatus'], to: TimelineEntry['toStatus'], actorId: string, message?: string): TimelineEntry => ({
  fromStatus: from,
  toStatus: to,
  actorId,
  actorName: actorId === aisyah ? 'Aisyah Rahman' : 'Farid Hassan',
  message: message ?? null,
  occurredAt: '2026-11-07T13:14:00Z',
});

const story: TimelineEntry[] = [
  line(null, 'SUBMITTED', aisyah),
  line('SUBMITTED', 'UNDER_REVIEW', farid),
  line('UNDER_REVIEW', 'MORE_INFO_NEEDED', farid, "Please upload the airline's delay report."),
  line('MORE_INFO_NEEDED', 'UNDER_REVIEW', aisyah, 'Here is the report.'),
  line('UNDER_REVIEW', 'APPROVED', farid, 'Receipts total RM 400.'),
];

group('describe', () => {
  it('says "you" to the person looking and names everyone else', () => {
    expect(story.map((entry) => describe(entry, aisyah))).toEqual([
      'Submitted by you',
      'Review started by Farid Hassan',
      'More information requested by Farid Hassan',
      'You sent more information',
      'Approved by Farid Hassan',
    ]);
    expect(describe(story[0], farid)).toBe('Submitted by Aisyah Rahman');
    expect(describe(story[3], farid)).toBe('Aisyah Rahman sent more information');
    expect(describe(story[1], farid)).toBe('Review started by you');
  });
});

group('timelineItems', () => {
  it('adds the steps still to come while the claim is open', () => {
    const items = timelineItems({ status: 'SUBMITTED', timeline: story.slice(0, 1) }, aisyah);
    expect(items.map((item) => [item.title, Boolean(item.upcoming)])).toEqual([
      ['Submitted by you', false],
      ['Review', true],
      ['Decision', true],
    ]);
  });

  it('adds nothing after a decision, and keeps each message as the detail', () => {
    const items = timelineItems({ status: 'APPROVED', timeline: story }, aisyah);
    expect(items).toHaveLength(5);
    expect(items[4].detail).toBe('Receipts total RM 400.');
  });
});

group('latestRequest', () => {
  it('finds the most recent request for more information', () => {
    const twoRounds = [...story.slice(0, 4), line('UNDER_REVIEW', 'MORE_INFO_NEEDED', farid, 'And the boarding pass.')];
    expect(latestRequest({ timeline: twoRounds })?.message).toBe('And the boarding pass.');
    expect(latestRequest({ timeline: story.slice(0, 2) })).toBeUndefined();
  });
});
