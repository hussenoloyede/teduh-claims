// Which navigation item is "current" on each page, worked out from the address.

import type { AgentNavItem } from '../components/AgentHeader/AgentHeader';
import type { CustomerSection } from '../components/BottomNav/BottomNav';

/** Which bottom tab a customer page belongs to. None for pages outside the four, such as notifications. */
export function sectionOf(pathname: string): CustomerSection | undefined {
  if (pathname === '/') return 'home';
  if (pathname.startsWith('/claims')) return 'claims';
  if (pathname.startsWith('/policies')) return 'policies';
  if (pathname.startsWith('/profile')) return 'profile';
  return undefined;
}

const agentPages = [
  { label: 'Claims queue', href: '/agent/queue' },
  { label: 'Assigned reviews', href: '/agent/assigned' },
  { label: 'History', href: '/agent/history' },
];

/** The agent header's links, with the one for this page marked as current. */
export function agentNav(pathname: string): AgentNavItem[] {
  return agentPages.map((page) => ({ ...page, current: pathname.startsWith(page.href) }));
}
