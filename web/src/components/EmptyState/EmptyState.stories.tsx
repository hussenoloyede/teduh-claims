import type { Meta, StoryObj } from '@storybook/react-vite';
import { FileText } from 'lucide-react';
import { expect } from 'storybook/test';
import { ButtonLink } from '../Button/Button';
import { EmptyState } from './EmptyState';

const meta = {
  title: 'Components/EmptyState',
  component: EmptyState,
  args: {
    icon: <FileText size={28} />,
    title: "You haven't made a claim yet",
    children: 'If something went wrong on your trip, start a claim and attach your receipts.',
  },
  decorators: [(Story) => <div style={{ maxWidth: 358 }}><Story /></div>],
} satisfies Meta<typeof EmptyState>;

export default meta;
type Story = StoryObj<typeof meta>;

export const NoClaims: Story = {};

export const WithAction: Story = {
  args: { action: <ButtonLink to="/claims/new">Start a claim</ButtonLink> },
  play: async ({ canvas }) => {
    await expect(canvas.getByRole('link', { name: 'Start a claim' })).toHaveAttribute('href', '/claims/new');
  },
};
