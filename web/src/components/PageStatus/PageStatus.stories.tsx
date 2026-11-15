import type { Meta, StoryObj } from '@storybook/react-vite';
import { expect, fn } from 'storybook/test';
import { PageError, PageLoading } from './PageStatus';

const meta = {
  title: 'Components/PageStatus',
  component: PageLoading,
} satisfies Meta<typeof PageLoading>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Loading: Story = {
  play: async ({ canvas }) => {
    await expect(canvas.getByRole('status')).toHaveTextContent('Loading Teduh');
  },
};

export const Failed: StoryObj<typeof PageError> = {
  args: { message: "Couldn't reach Teduh. Check your connection and try again.", onRetry: fn() },
  render: (args) => <PageError {...args} />,
  play: async ({ args, canvas, userEvent }) => {
    await userEvent.click(canvas.getByRole('button', { name: 'Try again' }));
    await expect(args.onRetry).toHaveBeenCalledOnce();
  },
};
