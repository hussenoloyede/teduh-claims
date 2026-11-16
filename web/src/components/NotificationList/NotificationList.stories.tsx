import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect, fn } from 'storybook/test';
import { NotificationList } from './NotificationList';

const claim = '/claims/0b7e6f2a-1c3d-4e5f-8a9b-0c1d2e3f4a5b';

const meta = {
  title: 'Components/NotificationList',
  component: NotificationList,
  args: {
    onOpen: fn(),
    notifications: [
      {
        id: 'n4',
        message: 'Your claim CLM-2026-000017 was approved. See the claim page for the amount.',
        createdAt: '2026-11-10T02:05:00Z',
        read: false,
        href: claim,
      },
      {
        id: 'n3',
        message: 'Your agent needs more information for claim CLM-2026-000017. See the claim page for what to send.',
        createdAt: '2026-11-08T02:30:00Z',
        read: true,
        href: claim,
      },
      {
        id: 'n1',
        message: "We've received your claim CLM-2026-000017. An agent will start reviewing it soon.",
        createdAt: '2026-11-07T13:14:00Z',
        read: true,
        href: claim,
      },
    ],
  },
  decorators: [(Story) => <div style={{ maxWidth: 358 }}><Story /></div>],
} satisfies Meta<typeof NotificationList>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Mixed: Story = {
  play: async ({ args, canvas, userEvent }) => {
    const links = canvas.getAllByRole('link');
    await expect(links[0]).toHaveTextContent('New');
    await expect(links[0]).toHaveTextContent('10 Nov 2026, 10:05 am');
    await expect(links[1]).not.toHaveTextContent('New');
    await userEvent.click(links[1]);
    await expect(args.onOpen).not.toHaveBeenCalled();
    await userEvent.click(links[0]);
    await expect(args.onOpen).toHaveBeenCalledWith('n4');
  },
};

export const AllRead: Story = {
  args: {
    notifications: [
      {
        id: 'n2',
        message: 'An agent has started reviewing your claim CLM-2026-000017.',
        createdAt: '2026-11-08T01:00:00Z',
        read: true,
        href: claim,
      },
    ],
  },
};
