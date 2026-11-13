import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect } from 'storybook/test';
import { claimStatuses } from '../../api/claims';
import { StatusBadge } from './StatusBadge';

const meta = {
  title: 'Components/StatusBadge',
  component: StatusBadge,
  args: { status: 'SUBMITTED' },
  argTypes: {
    status: { control: 'select', options: claimStatuses },
  },
} satisfies Meta<typeof StatusBadge>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Submitted: Story = {};

export const UnderReview: Story = {
  args: { status: 'UNDER_REVIEW' },
  // Checks the words reach the page as written, not in capitals.
  play: async ({ canvas }) => {
    await expect(canvas.getByText('Under review')).toBeVisible();
  },
};

export const MoreInfoNeeded: Story = {
  args: { status: 'MORE_INFO_NEEDED' },
};

export const Approved: Story = {
  args: { status: 'APPROVED' },
};

export const Rejected: Story = {
  args: { status: 'REJECTED' },
};

const allStatuses = () => (
  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--space-2)' }}>
    {claimStatuses.map((status) => (
      <StatusBadge key={status} status={status} />
    ))}
  </div>
);

export const AllCustomer: Story = {
  render: allStatuses,
  globals: { theme: 'customer' },
};

export const AllAgent: Story = {
  render: allStatuses,
  globals: { theme: 'agent' },
};
