import { useQuery } from '@tanstack/react-query';
import { claimTypeLabel, type ClaimType } from './claims';
import { api, unwrap } from './client';
import type { components } from './schema';

export type Policy = components['schemas']['PolicyCover'];

export const policiesQueryKey = ['policies'] as const;

/** The signed-in customer's policies with their limits. They only change when the customer buys a plan. */
export function usePolicies() {
  return useQuery({
    queryKey: policiesQueryKey,
    queryFn: () => unwrap(api.GET('/api/policies')),
    staleTime: Infinity,
  });
}

export type CoverState = 'active' | 'upcoming' | 'ended';

/**
 * Whether the policy covers today. Dates are compared as "2026-11-16" strings, which sort the same way as
 * the dates they stand for, and both ends count as covered, as in PolicyCover.covers() in Java.
 */
export function coverState(policy: Pick<Policy, 'coverStart' | 'coverEnd'>, today: string): CoverState {
  if (today < policy.coverStart) return 'upcoming';
  if (today > policy.coverEnd) return 'ended';
  return 'active';
}

/** The claim types a policy covers, with their limits, in the same order as the claim type labels. */
export function limitsOf(policy: Pick<Policy, 'limits'>): { type: ClaimType; label: string; limit: number }[] {
  return (Object.keys(claimTypeLabel) as ClaimType[])
    .filter((type) => policy.limits[type] !== undefined)
    .map((type) => ({ type, label: claimTypeLabel[type], limit: policy.limits[type] }));
}

/** Plan names by policy id, so a claim list can name each claim's plan. */
export function planNames(policies: Policy[] | undefined): Map<string, string> {
  return new Map((policies ?? []).map((policy) => [policy.policyId, policy.planName]));
}
