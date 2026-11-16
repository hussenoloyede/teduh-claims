import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect } from 'storybook/test';
import { Button } from '../Button/Button';
import { Banner } from './Banner';

const meta = {
  title: 'Components/Banner',
  component: Banner,
  args: { variant: 'info', title: 'What to attach', children: 'Receipts, and any report from the airline.' },
  decorators: [(Story) => <div style={{ maxWidth: 358 }}><Story /></div>],
} satisfies Meta<typeof Banner>;

export default meta;
type Story = StoryObj<typeof meta>;

export const InfoBanner: Story = {};

export const Submitted: Story = {
  args: {
    variant: 'success',
    title: 'Claim CLM-2026-000017 submitted',
    children: "You'll get a notification when an agent starts reviewing it.",
  },
  play: async ({ canvas }) => {
    await expect(canvas.getByRole('status')).toHaveTextContent('CLM-2026-000017');
  },
};

export const Warning: Story = {
  args: { variant: 'warning', title: 'Your agent needs more information', children: 'Send the airline delay report.' },
};

export const Conflict: Story = {
  args: {
    variant: 'error',
    title: 'This claim changed while you were looking at it',
    children: 'Reload to see the latest.',
    action: <Button size="sm" variant="secondary">Reload</Button>,
  },
  play: async ({ canvas }) => {
    await expect(canvas.getByRole('alert')).toHaveTextContent('changed while you were looking');
  },
};
