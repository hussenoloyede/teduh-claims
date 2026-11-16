import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect } from 'storybook/test';
import { PolicyCard } from './PolicyCard';

const meta = {
  title: 'Components/PolicyCard',
  component: PolicyCard,
  args: {
    planName: 'Teduh Travel Plus',
    policyNumber: 'TT-2026-004218',
    coverStart: '2026-10-01',
    coverEnd: '2026-12-31',
    state: 'active',
    covers: ['Flight delay', 'Baggage delayed', 'Lost or damaged baggage', 'Trip cancellation'],
    href: '/policies',
  },
  decorators: [(Story) => <div style={{ maxWidth: 358 }}><Story /></div>],
} satisfies Meta<typeof PolicyCard>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Active: Story = {
  play: async ({ canvas }) => {
    await expect(canvas.getByText('Covered 1 Oct 2026 to 31 Dec 2026')).toBeVisible();
    await expect(canvas.getByRole('link', { name: 'Teduh Travel Plus details' })).toHaveAttribute('href', '/policies');
  },
};

export const Ended: Story = {
  args: { state: 'ended', coverStart: '2026-01-01', coverEnd: '2026-03-31', href: undefined },
};

export const NotStarted: Story = {
  args: { state: 'upcoming', coverStart: '2027-01-01', coverEnd: '2027-03-31' },
};
