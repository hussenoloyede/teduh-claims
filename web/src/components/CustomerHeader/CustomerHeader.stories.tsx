import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect } from 'storybook/test';
import { CustomerHeader } from './CustomerHeader';

const meta = {
  title: 'Components/CustomerHeader',
  component: CustomerHeader,
  args: { unreadCount: 2, userName: 'Aisyah Rahman' },
  decorators: [(Story) => <div style={{ maxWidth: 390 }}><Story /></div>],
} satisfies Meta<typeof CustomerHeader>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Home: Story = {
  play: async ({ canvas }) => {
    await expect(canvas.getByRole('link', { name: 'Notifications, 2 unread' })).toBeVisible();
    await expect(canvas.getByRole('link', { name: 'Profile' })).toHaveTextContent('AR');
  },
};

export const NothingUnread: Story = {
  args: { unreadCount: 0 },
  play: async ({ canvas }) => {
    await expect(canvas.getByRole('link', { name: 'Notifications' })).toBeVisible();
  },
};

export const WithBack: Story = {
  args: { backHref: '/claims', userName: undefined },
};
