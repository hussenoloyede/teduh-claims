import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect, fn } from 'storybook/test';
import { AgentHeader } from './AgentHeader';

const meta = {
  title: 'Components/AgentHeader',
  component: AgentHeader,
  args: {
    nav: [
      { label: 'Claims queue', href: '/agent/queue', current: true },
      { label: 'Assigned reviews', href: '/agent/assigned' },
      { label: 'History', href: '/agent/history' },
    ],
    unreadCount: 2,
    userName: 'Farid Hassan',
    userRole: 'Claims agent',
    onSignOut: fn(),
  },
  globals: { theme: 'agent' },
} satisfies Meta<typeof AgentHeader>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Queue: Story = {
  play: async ({ args, canvas, userEvent }) => {
    await expect(canvas.getByRole('link', { name: 'Claims queue' })).toHaveAttribute('aria-current', 'page');
    await expect(canvas.getByRole('link', { name: 'Notifications, 2 unread' })).toBeVisible();
    await userEvent.click(canvas.getByRole('button', { name: 'Sign out' }));
    await expect(args.onSignOut).toHaveBeenCalledOnce();
  },
};

export const NothingUnread: Story = {
  args: { unreadCount: 0 },
};
