import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect, within } from 'storybook/test';
import { ClaimTable, type ClaimTableRow } from './ClaimTable';

const queue: ClaimTableRow[] = [
  { id: 'c17', reference: 'CLM-2026-000017', customerName: 'Aisyah Rahman', claimType: 'Baggage delayed', amount: 450, status: 'SUBMITTED', when: '3 days', href: '/agent/claims/c17', actionLabel: 'Open claim' },
  { id: 'c19', reference: 'CLM-2026-000019', customerName: 'Daniel Tan', claimType: 'Flight delay', amount: 300, status: 'SUBMITTED', when: '2 days', href: '/agent/claims/c19', actionLabel: 'Open claim' },
  { id: 'c22', reference: 'CLM-2026-000022', customerName: 'Daniel Tan', claimType: 'Trip cancellation', amount: 4200, status: 'SUBMITTED', when: '5 hours', href: '/agent/claims/c22', actionLabel: 'Open claim' },
];

const meta = {
  title: 'Components/ClaimTable',
  component: ClaimTable,
  globals: { theme: 'agent' },
  parameters: { layout: 'padded' },
} satisfies Meta<typeof ClaimTable>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Queue: Story = {
  args: { caption: 'Claims waiting for review', whenLabel: 'Waiting', rows: queue, highlightFirst: true },
  play: async ({ canvas }) => {
    const table = canvas.getByRole('table', { name: 'Claims waiting for review' });
    const rows = within(table).getAllByRole('row');
    await expect(rows).toHaveLength(4);
    await expect(within(rows[1]).getByRole('rowheader')).toHaveTextContent('CLM-2026-000017');
    await expect(canvas.getByRole('link', { name: 'Open claim CLM-2026-000017' })).toHaveAttribute(
      'href',
      '/agent/claims/c17',
    );
  },
};

export const History: Story = {
  args: {
    caption: 'Claims you have decided',
    whenLabel: 'Submitted',
    rows: [
      { id: 'c17', reference: 'CLM-2026-000017', customerName: 'Aisyah Rahman', claimType: 'Baggage delayed', amount: 400, claimedAmount: 450, status: 'APPROVED', when: '7 Nov 2026', href: '/agent/claims/c17', actionLabel: 'View claim' },
      { id: 'c9', reference: 'CLM-2026-000009', customerName: 'Aisyah Rahman', claimType: 'Trip cancellation', amount: 2100, claimedAmount: 2100, status: 'REJECTED', when: '21 Oct 2026', href: '/agent/claims/c9', actionLabel: 'View claim' },
    ],
  },
  play: async ({ canvas }) => {
    await expect(canvas.getByText('of RM 450.00')).toBeVisible();
    await expect(canvas.queryByText('of RM 2,100.00')).toBeNull();
  },
};
