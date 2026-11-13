import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect } from 'storybook/test';
import { Timeline } from './Timeline';

const meta = {
  title: 'Components/Timeline',
  component: Timeline,
  decorators: [
    (Story) => (
      <div style={{ maxWidth: 326, padding: 'var(--space-4)', background: 'var(--color-bg)', borderRadius: 'var(--radius-lg)' }}>
        <Story />
      </div>
    ),
  ],
} satisfies Meta<typeof Timeline>;

export default meta;
type Story = StoryObj<typeof meta>;

export const JustSubmitted: Story = {
  args: {
    items: [
      { id: '1', title: 'Submitted by you', time: '2026-11-07T13:14:00Z' },
      { id: '2', title: 'Review', upcoming: true },
      { id: '3', title: 'Decision', upcoming: true },
    ],
  },
  play: async ({ canvas }) => {
    await expect(canvas.getAllByRole('listitem')).toHaveLength(3);
    await expect(canvas.getByText('7 Nov 2026, 9:14 pm')).toBeVisible();
  },
};

export const WithInfoRequest: Story = {
  args: {
    items: [
      { id: '1', title: 'Submitted by you', time: '2026-11-07T13:14:00Z' },
      { id: '2', title: 'Review started by Farid', time: '2026-11-08T02:05:00Z' },
      {
        id: '3',
        title: 'More information requested',
        time: '2026-11-08T02:20:00Z',
        detail: 'Please upload the letter from the airline confirming the delay.',
      },
      { id: '4', title: 'You replied with 1 file', time: '2026-11-08T09:41:00Z' },
      {
        id: '5',
        title: 'Approved for RM 400.00',
        time: '2026-11-09T03:10:00Z',
        detail: 'The pharmacy receipt is outside the delay period, so it is not covered.',
      },
    ],
  },
};

// The same claim as the agent sees it, so "you" is now the agent.
export const Agent: Story = {
  args: {
    items: [
      { id: '1', title: 'Submitted by Aisyah Rahman', time: '2026-11-07T13:14:00Z' },
      { id: '2', title: 'Review started by you', time: '2026-11-08T02:05:00Z' },
      {
        id: '3',
        title: 'More information requested',
        time: '2026-11-08T02:20:00Z',
        detail: 'Please upload the letter from the airline confirming the delay.',
      },
      { id: '4', title: 'Aisyah replied with 1 file', time: '2026-11-08T09:41:00Z' },
      { id: '5', title: 'Decision', upcoming: true },
    ],
  },
  globals: { theme: 'agent' },
};
