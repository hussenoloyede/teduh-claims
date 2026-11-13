import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect } from 'storybook/test';
import { ClaimCard } from './ClaimCard';

const meta = {
  title: 'Components/ClaimCard',
  component: ClaimCard,
  args: {
    reference: 'CLM-2026-000024',
    claimType: 'FLIGHT_DELAY',
    amountClaimed: 300,
    status: 'SUBMITTED',
    submittedAt: '2026-11-12T02:40:00Z',
    policyName: 'Teduh Travel Plus',
    href: '/claims/example',
  },
  // The customer screens are 390 px wide with 16 px margins.
  decorators: [(Story) => <div style={{ maxWidth: 358 }}><Story /></div>],
} satisfies Meta<typeof ClaimCard>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Submitted: Story = {
  play: async ({ canvas }) => {
    const link = canvas.getByRole('link');
    await expect(link).toHaveAttribute('href', '/claims/example');
    await expect(link).toHaveTextContent('RM 300.00');
    await expect(link).toHaveTextContent('12 Nov 2026');
  },
};

export const MoreInfoNeeded: Story = {
  args: {
    reference: 'CLM-2026-000017',
    claimType: 'BAGGAGE_DELAYED',
    amountClaimed: 450,
    status: 'MORE_INFO_NEEDED',
    submittedAt: '2026-11-07T13:14:00Z',
  },
};

export const Rejected: Story = {
  args: {
    reference: 'CLM-2026-000009',
    claimType: 'TRIP_CANCELLATION',
    amountClaimed: 2100,
    status: 'REJECTED',
    submittedAt: '2026-10-20T04:05:00Z',
  },
};

export const List: Story = {
  render: (args) => (
    <div style={{ display: 'grid', gap: 'var(--space-3)' }}>
      <ClaimCard {...args} />
      <ClaimCard
        {...args}
        reference="CLM-2026-000017"
        claimType="BAGGAGE_DELAYED"
        amountClaimed={450}
        status="APPROVED"
        submittedAt="2026-11-07T13:14:00Z"
      />
      <ClaimCard
        {...args}
        reference="CLM-2026-000009"
        claimType="TRIP_CANCELLATION"
        amountClaimed={2100}
        status="REJECTED"
        submittedAt="2026-10-20T04:05:00Z"
      />
    </div>
  ),
};
