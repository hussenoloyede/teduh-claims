import { useQuery } from '@tanstack/react-query';
import { api, unwrap } from './client';
import type { components } from './schema';

export type PlanOffer = components['schemas']['PlanOffer'];
export type TravelPlan = PlanOffer['plan'];

/** The three plans with their demo prices. They are the same for everyone and never change during a visit. */
export function usePlans() {
  return useQuery({
    queryKey: ['plans'],
    queryFn: () => unwrap(api.GET('/api/plans')),
    staleTime: Infinity,
  });
}

/** "Teduh Travel Plus" → "Plus", for short labels such as "Continue with Plus". */
export function shortPlanName(name: string): string {
  return name.replace(/^Teduh Travel /, '');
}
